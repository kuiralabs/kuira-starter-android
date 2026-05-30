package com.kuiralabs.starter.counter.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// Phase 4 placeholder ViewModel. Holds the CounterUiState and exposes
// no-op transitions. Phase 5 replaces this with:
//   - SigilSession / WalletPanelViewModel observation to set NotReady
//     vs ReadyToDeploy automatically.
//   - MidnightContract.deploy() wiring with progress callbacks.
//   - MidnightContract.call("increment") + ledger().getUint64("count").
//   - Indexer GraphQL subscription for live count.
//   - Per-network EncryptedSharedPreferences for the deployed address.
@HiltViewModel
class CounterViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<CounterUiState>(CounterUiState.NotReady)
    val state: StateFlow<CounterUiState> = _state.asStateFlow()
}
