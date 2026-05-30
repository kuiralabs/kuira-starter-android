package com.kuiralabs.starter.counter.ui

// Counter card's UI state. Three branches:
//
//   NotReady       — no sigil yet, or wallet has no dust. The user
//                    has to take action above (forge, fund, register
//                    dust) before this card becomes interactive.
//
//   ReadyToDeploy  — sigil exists, wallet has dust, no contract yet
//                    deployed for the current network. Card surfaces
//                    a "Deploy counter" button.
//
//   Deployed       — a counter is deployed on the current network.
//                    Card renders the count and an Increment button.
sealed interface CounterUiState {
    data object NotReady : CounterUiState
    data object ReadyToDeploy : CounterUiState
    data class Deployed(val address: String, val count: Long?) : CounterUiState
}
