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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
// Plus a polling loop that re-reads count every COUNT_POLL_INTERVAL_MS
// while the card is in the Deployed state. The SDK doesn't expose a
// Flow-based subscription for contract state, so polling is the only
// consumer-level option; if a ledger Flow is added later, this loop
// collapses to a single collect.
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

    // Network selector lives on the SDK's WalletStatusPanel; this
    // ViewModel does not currently observe network changes. The
    // starter pins UNDEPLOYED (localnet) — multi-network coordination
    // is a future hook, not a starter feature.
    private val activeNetwork = MutableStateFlow(MidnightNetwork.UNDEPLOYED)

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            sdkProvider.sdk.combine(activeNetwork) { sdk, net -> sdk to net }
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
        if (sdk != null && persisted != null) startPolling(sdk, persisted) else stopPolling()
    }

    fun deploy() {
        val sdk = sdkProvider.sdk.value ?: return
        val network = activeNetwork.value
        runAction {
            val address = CounterContract.deploy(context, sdk) { _callStage.value = it }
            addressStore.put(network, address)
            recomputeState(sdk, network)
        }
    }

    fun increment() {
        val sdk = sdkProvider.sdk.value ?: return
        val address = (state.value as? CounterUiState.Deployed)?.address ?: return
        runAction {
            CounterContract.increment(context, sdk, address) { _callStage.value = it }
            val freshCount = CounterContract.readCount(readHandleFor(sdk, address))
            _state.update { CounterUiState.Deployed(address = address, count = freshCount) }
            // No need to recompute state here — the polling loop will
            // continue from the next tick with the same address.
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
    // and normalizing ES module syntax — re-doing that every 4s for
    // the polling loop is wasteful. Re-created only when the address
    // changes (deploy on a new network, restore on a fresh device).
    private var readHandle: MidnightContract? = null
    private var readHandleAddress: String? = null

    private fun readHandleFor(sdk: MidnightSdk, address: String): MidnightContract {
        if (readHandle == null || readHandleAddress != address) {
            readHandle = CounterContract.buildReadHandle(context, sdk, address)
            readHandleAddress = address
        }
        return readHandle!!
    }

    private fun startPolling(sdk: MidnightSdk, address: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val handle = readHandleFor(sdk, address)
            while (true) {
                try {
                    val fresh = CounterContract.readCount(handle)
                    _state.update { current ->
                        if (current is CounterUiState.Deployed && current.address == address) {
                            current.copy(count = fresh)
                        } else current
                    }
                } catch (_: Throwable) {
                    // Polling errors are non-fatal — next tick retries.
                    // We deliberately do NOT surface to _error here so
                    // a transient indexer hiccup doesn't flash an
                    // error banner under the user every few seconds.
                }
                delay(COUNT_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        readHandle = null
        readHandleAddress = null
    }

    companion object {
        // Localnet blocks land every ~3s; PREPROD every ~6s. 4s is the
        // middle of the road — a bit eager on PREPROD, a bit slow on
        // localnet, but a single value beats per-network tuning for a
        // starter.
        private const val COUNT_POLL_INTERVAL_MS = 4_000L
    }
}
