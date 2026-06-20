package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.midnight.kuira.dapp.PanelBar

// The starter's home screen. The sigil + wallet pills float over the content as
// draggable chips (PanelBar floating mode) rather than sitting inline — the
// recommended host integration. Underneath is just the headline + the CounterCard.
//
// The wallet pill owns network selection: its pick drives the SDK's durable
// NetworkPreferenceStore, which CounterViewModel follows (selectedNetwork) so the
// per-network contract address tracks whatever chain the wallet is on.
@Composable
fun CounterScreen(
    modifier: Modifier = Modifier,
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val selectedNetwork by viewModel.selectedNetwork.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // safeDrawing combines status bar + display cutout + nav bar, so the
                // headline never gets bisected by a camera notch or hidden under the bar.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                // Clear the floating pills docked at the top corners so the headline
                // isn't tucked under them on first launch.
                .padding(top = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Kuira Starter — Counter",
                style = MaterialTheme.typography.headlineMedium,
            )

            CounterCard()
        }

        // Sigil + wallet pills as draggable floaters (PanelBar floating mode),
        // overlaying the content — only the chips consume touch, so the CounterCard
        // stays interactive everywhere else. Placed LAST so it renders on top.
        PanelBar(
            floating = true,
            network = selectedNetwork,
            onNetworkChange = viewModel::selectNetwork,
        )
    }
}
