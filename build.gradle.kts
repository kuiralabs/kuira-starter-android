plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Localnet adb-reverse on physical-device installs is provided by the SDK's
// `io.github.kuiralabs.localnet` plugin, applied in app/build.gradle.kts.
