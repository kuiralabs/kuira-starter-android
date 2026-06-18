package com.kuiralabs.starter.counter

import android.app.Application
import com.midnight.kuira.sdk.walletruntime.SessionLock
import com.midnight.kuira.sdk.walletruntime.WalletForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KuiraStarterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Session auto-lock (#14): app-level triggers (background + screen-off).
        // Foreground idle is reset per-Activity via onUserInteraction.
        SessionLock.attach(this)
        // Keep wallet operations alive across backgrounding: shows a progress
        // notification for any in-flight op (dust sync, send, contract calls) and a
        // finalization push on completion; tears down on foreground / lock.
        WalletForegroundService.attach(this)
    }
}
