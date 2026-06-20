package com.kuiralabs.starter.counter.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuiralabs.starter.counter.data.ContractAddressStore
import com.kuiralabs.starter.counter.data.CounterContract
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import com.midnight.kuira.sdk.walletruntime.MidnightSdkProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Drives the CounterCard. Three responsibilities:
//
//   1. Sync the displayed CounterUiState with two upstream signals —
//      sdkProvider.sdk (StateFlow<MidnightSdk?>) and the current
//      network. NotReady when sdk is null; ReadyToDeploy when sdk
//      exists but no persisted address for the network; Deployed when
//      both are present.
//
//   2. deploy() — call MidnightContract.deploy(), persist the new
//      address per network, transition to Deployed.
//
//   3. increment() — call the increment circuit; on success, refresh
//      the visible count.
//
// Plus a reactive count stream: while the card is Deployed it collects
// CounterContract.observeCount (MidnightContract.observeLedger, #255), so the
// count updates on each on-chain change instead of on a 4s timer.
@HiltViewModel
class CounterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkProvider: MidnightSdkProvider,
    private val addressStore: ContractAddressStore,
) : ViewModel() {

    private val _state = MutableStateFlow<CounterUiState>(CounterUiState.NotReady)
    val state: StateFlow<CounterUiState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    // Live stage of the in-flight contract call (execute → prove →
    // balance → submit), fed straight from MidnightContract's
    // onProgress. Drives the SDK's ContractCallProgressBar. Null when
    // idle. A deploy/increment takes 30–120s, so a staged bar reads far
    // better than a bare spinner.
    private val _callStage = MutableStateFlow<ContractCallStage?>(null)
    val callStage: StateFlow<ContractCallStage?> = _callStage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // The active network is the SDK's durable preference (#285): the wallet pill in
    // PanelBar drives it, and the per-network contract address follows. The screen
    // seeds PanelBar from selectedNetwork and writes picks back through selectNetwork,
    // so switching network reloads the right contract (or ReadyToDeploy).
    val selectedNetwork: StateFlow<MidnightNetwork> get() = sdkProvider.selectedNetwork
    fun selectNetwork(network: MidnightNetwork) = sdkProvider.selectNetwork(network)

    private var countJob: Job? = null

    init {
        viewModelScope.launch {
            sdkProvider.sdk.combine(sdkProvider.selectedNetwork) { sdk, net -> sdk to net }
                .collect { (sdk, network) -> recomputeState(sdk, network) }
        }
    }

    private fun recomputeState(sdk: MidnightSdk?, network: MidnightNetwork) {
        val persisted = addressStore.get(network)
        val next = when {
            sdk == null -> CounterUiState.NotReady
            persisted == null -> CounterUiState.ReadyToDeploy
            else -> CounterUiState.Deployed(address = persisted, count = null)
        }
        _state.value = next
        if (sdk != null && persisted != null) startObserving(sdk, persisted) else stopObserving()
    }

    fun deploy() {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        runAction {
            val address = CounterContract.deploy(context, sdk) { _callStage.value = it }
            addressStore.put(network, address)
            recomputeState(sdk, network)
        }
    }

    // Forget the current contract → ReadyToDeploy, so the next deploy() makes a
    // FRESH counter (starts at 0). The on-chain contract isn't destroyed — we
    // just stop pointing at it. Use to abandon a contract you no longer want
    // (e.g. after a localnet reset left a stale address).
    fun disconnect() {
        val network = sdkProvider.selectedNetwork.value
        addressStore.clear(network)
        recomputeState(sdkProvider.sdk.value, network)
    }

    fun increment() {
        val sdk = sdkProvider.sdk.value ?: return
        val address = (state.value as? CounterUiState.Deployed)?.address ?: return
        runAction {
            CounterContract.increment(context, sdk, address) { _callStage.value = it }
            val freshCount = CounterContract.readCount(readHandleFor(sdk, address))
            _state.update { CounterUiState.Deployed(address = address, count = freshCount) }
            // No need to recompute state here — the count stream keeps
            // running for the same address and reflects later changes too.
        }
    }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                block()
            } catch (t: Throwable) {
                _error.value = t.message ?: t::class.simpleName ?: "Unknown error"
            } finally {
                _busy.value = false
                _callStage.value = null
            }
        }
    }

    // Cached read-only MidnightContract for the currently-deployed
    // address. Building one means opening the contract JS asset stream
    // and normalizing ES module syntax — the count stream + the
    // post-increment read share one handle rather than rebuilding it.
    // Re-created only when the address changes (deploy on a new network,
    // restore on a fresh device).
    private var readHandle: MidnightContract? = null
    private var readHandleAddress: String? = null

    private fun readHandleFor(sdk: MidnightSdk, address: String): MidnightContract {
        if (readHandle == null || readHandleAddress != address) {
            readHandle = CounterContract.buildReadHandle(context, sdk, address)
            readHandleAddress = address
        }
        return readHandle!!
    }

    private fun startObserving(sdk: MidnightSdk, address: String) {
        countJob?.cancel()
        countJob = viewModelScope.launch {
            val handle = readHandleFor(sdk, address)
            CounterContract.observeCount(handle)
                // observeLedger is resilient (skips transient indexer hiccups, degrades to
                // polling if the block stream drops), so a thrown terminal error is rare; if it
                // happens we just stop this stream — the next recompute restarts it. We do NOT
                // surface it to _error: no error banner for a background read.
                .catch { /* non-fatal */ }
                .collect { fresh ->
                    _state.update { current ->
                        if (current is CounterUiState.Deployed && current.address == address) {
                            current.copy(count = fresh)
                        } else current
                    }
                }
        }
    }

    private fun stopObserving() {
        countJob?.cancel()
        countJob = null
        readHandle = null
        readHandleAddress = null
    }
}
