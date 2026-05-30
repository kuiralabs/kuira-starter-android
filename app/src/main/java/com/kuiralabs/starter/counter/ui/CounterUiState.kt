package com.kuiralabs.starter.counter.ui

// Counter card's UI state. Three branches:
//
//   NotReady       — the SDK isn't bootstrapped yet (sigil not forged
//                    or wallet runtime not ready). User completes the
//                    Sigil + Wallet panels above; this card flips to
//                    ReadyToDeploy automatically.
//
//   ReadyToDeploy  — SDK is bootstrapped, no contract yet deployed for
//                    the current network. Card surfaces "Deploy counter".
//                    NOTE: an actual deploy still needs NIGHT + DUST in
//                    the wallet — if the user taps Deploy before
//                    funding via the wallet panel, the deploy call
//                    surfaces a clear error rather than gating the
//                    button. Keeps the state machine flat.
//
//   Deployed       — a counter is deployed on the current network.
//                    Card renders the count and an Increment button.
sealed interface CounterUiState {
    data object NotReady : CounterUiState
    data object ReadyToDeploy : CounterUiState
    data class Deployed(val address: String, val count: Long?) : CounterUiState
}
