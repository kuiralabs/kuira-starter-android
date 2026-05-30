package com.kuiralabs.starter.counter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not ComponentActivity) because SigilStatusPanel hosts
// a biometric prompt internally and the prompt requires a FragmentActivity
// host — AppCompatActivity satisfies that; ComponentActivity does not.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaceholderScreen()
                }
            }
        }
    }
}

// Phase 3 placeholder — Phase 4 replaces this with the real counter screen
// (SigilStatusPanel + balance pill + count + increment button).
@Composable
private fun PlaceholderScreen() {
    Text(
        text = "Kuira Starter — Counter\n\n(Phase 3 of 6 complete: scaffolding compiles.)",
        modifier = Modifier.padding(24.dp),
    )
}
