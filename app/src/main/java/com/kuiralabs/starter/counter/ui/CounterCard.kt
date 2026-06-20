package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.midnight.kuira.dapp.ContractCallProgressBar

// CounterCard — the starter's custom UI for the on-chain counter.
// Branches on CounterUiState; the busy flag from the ViewModel
// disables buttons + shows a spinner while a deploy or increment
// transaction is in flight.
@Composable
fun CounterCard(
    modifier: Modifier = Modifier,
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val callStage by viewModel.callStage.collectAsState()
    val error by viewModel.error.collectAsState()

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

            when (val s = state) {
                CounterUiState.NotReady -> NotReadyBody()
                CounterUiState.ReadyToDeploy -> ReadyToDeployBody(busy = busy, onDeploy = viewModel::deploy)
                is CounterUiState.Deployed -> DeployedBody(
                    state = s,
                    busy = busy,
                    onIncrement = viewModel::increment,
                    onDeployNew = viewModel::deploy,
                    onDisconnect = viewModel::disconnect,
                )
            }

            // While a transaction is in flight, show the SDK's staged
            // progress bar (execute → prove → balance → submit) fed by
            // the live ContractCallStage. Before the first stage lands
            // it's null, so fall back to a spinner for that brief gap.
            if (busy) {
                if (callStage == null) {
                    CircularProgressIndicator()
                } else {
                    ContractCallProgressBar(
                        stage = callStage,
                        accent = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (error != null) {
                Text(
                    text = "Last error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun NotReadyBody() {
    Text(
        text = "Forge a sigil above, then fund the wallet and register dust.\n\n" +
            "On localnet, fund + register-dust steps are:\n" +
            "    mn airdrop 1000 --wallet <addr> --network undeployed\n" +
            "    mn dust register --wallet <addr> --network undeployed\n\n" +
            "(Copy the wallet address from the panel above.)",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ReadyToDeployBody(busy: Boolean, onDeploy: () -> Unit) {
    Text(
        text = "Sigil + funded wallet detected. Deploy a counter contract on " +
            "the current network. The deployed address persists in " +
            "EncryptedSharedPreferences so subsequent runs reuse it.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onDeploy, enabled = !busy) {
        Text(text = "Deploy counter")
    }
}

@Composable
private fun DeployedBody(
    state: CounterUiState.Deployed,
    busy: Boolean,
    onIncrement: () -> Unit,
    onDeployNew: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Text(
        text = "Deployed at:\n${state.address}",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = state.count?.toString() ?: "—",
        style = MaterialTheme.typography.displayLarge,
    )
    Button(onClick = onIncrement, enabled = !busy) {
        Text(text = "Increment")
    }
    // Secondary actions: spin up a fresh counter (new address, starts at 0),
    // or just forget this one (e.g. after a localnet reset stranded the address).
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onDeployNew, enabled = !busy) {
            Text(text = "Deploy new")
        }
        TextButton(onClick = onDisconnect, enabled = !busy) {
            Text(text = "Disconnect")
        }
    }
    Text(
        text = "Count updates live from chain.",
        style = MaterialTheme.typography.bodySmall,
    )
}
