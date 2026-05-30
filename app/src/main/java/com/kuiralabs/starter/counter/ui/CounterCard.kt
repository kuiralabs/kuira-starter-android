package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

// Counter card — the starter's custom UI surface for the on-chain count.
//
// Phase 4 (this file): renders a state-aware card that branches on
// CounterUiState. The Deploy and Increment actions are stubs that
// flip ViewModel state without touching the chain.
//
// Phase 5: replaces the stubs with real MidnightContract.deploy() and
// .call() invocations, persists the deployed address per network, and
// subscribes to ledger state via the indexer for live count sync.
@Composable
fun CounterCard(
    modifier: Modifier = Modifier,
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Counter contract",
                style = MaterialTheme.typography.titleMedium,
            )
            CounterCardBody(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun CounterCardBody(
    state: CounterUiState,
    viewModel: CounterViewModel,
) {
    when (state) {
        CounterUiState.NotReady -> Text(
            text = "Forge a sigil and register dust above first.",
            style = MaterialTheme.typography.bodyMedium,
        )

        CounterUiState.ReadyToDeploy -> Text(
            text = "(Phase 5 will wire MidnightContract.deploy() here. " +
                "Tap target: deploy a fresh counter on the current network and " +
                "persist its address in EncryptedSharedPreferences.)",
            style = MaterialTheme.typography.bodyMedium,
        )

        is CounterUiState.Deployed -> Text(
            text = "(Phase 5 will subscribe to ledger state here. " +
                "Deployed address: ${state.address}. Render count + Increment button.)",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
