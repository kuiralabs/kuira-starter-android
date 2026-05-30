package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.midnight.kuira.dapp.sigil.SigilStatusPanel
import com.midnight.kuira.dapp.wallet.WalletStatusPanel

// The starter's home screen — three vertically stacked sections:
//
//   1. SigilStatusPanel  — SDK-provided Compose component. Owns the
//      identity lifecycle (Forge / Restore / Forged) and renders the
//      DID + biometric-prompt entry points.
//
//   2. WalletStatusPanel  — SDK-provided Compose component. Owns the
//      wallet lifecycle (Fund / Register dust / Ready) and renders the
//      receive QR + balance pill + dust registration button.
//
//   3. CounterCard  — this starter's custom card. Owns the contract
//      lifecycle (Deploy / Increment) and renders the on-chain count.
//
// The screen does NOT manage state across the three sections — each
// panel owns its own ViewModel and StateFlow and is composable
// independently. The starter shows the raw composition deliberately
// so consumers can see how the pieces fit together before they reach
// for a more opinionated host wrapper.
@Composable
fun CounterScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Kuira Starter — Counter",
            style = MaterialTheme.typography.headlineMedium,
        )

        SigilStatusPanel()

        WalletStatusPanel()

        CounterCard()
    }
}
