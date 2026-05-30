package com.kuiralabs.starter.counter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kuiralabs.starter.counter.ui.CounterScreen
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
                    CounterScreen()
                }
            }
        }
    }
}
