package com.kuiralabs.starter.counter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kuiralabs.starter.counter.ui.CounterScreen
import com.midnight.kuira.dapp.lock.SessionLockGate
import com.midnight.kuira.sdk.walletruntime.WalletNotifications
import com.midnight.kuira.sdk.walletruntime.SessionLock
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity (not ComponentActivity) because SigilStatusPanel hosts
// a biometric prompt internally and the prompt requires a FragmentActivity
// host — AppCompatActivity satisfies that; ComponentActivity does not.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    /** Session auto-lock (#14) — onUserInteraction resets its foreground idle timer. */
    @Inject lateinit var sessionLock: SessionLock

    // POST_NOTIFICATIONS for the dust-sync Live Update (#235). Best-effort: if the
    // user denies, the background sync still runs — just without a visible notification.
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (WalletNotifications.shouldRequest(this)) {
            notifPermission.launch(WalletNotifications.PERMISSION)
        }
        setContent {
            SessionLockGate {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        CounterScreen()
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionLock.onUserActivity()
    }
}
