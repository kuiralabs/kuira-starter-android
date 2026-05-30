import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.kuiralabs.starter.counter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kuiralabs.starter.counter"
        // Kuira SDK requires minSdk 30 (Block Store API, passkey
        // CredentialManager, Android 11+ scoped storage assumptions).
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// ─── Contract assets sync ──────────────────────────────────────────
// Copy ../contract/src/managed/counter/ into the app's assets/ so the
// SDK's MidnightContract.fromAssets() can load index.js + the proving
// keys at runtime.
//
// Why hand-rolled (not the com.midnight.kuira.contract plugin):
// the plugin was authored during alpha02 and has not shipped to
// Maven Central as of the kuira-starter-android repo's alpha01 pin.
// When it ships, swap this whole block for the one-liner:
//
//   plugins { id("com.midnight.kuira.contract") version "0.1.0-alpha02" }
//
// and the plugin will discover ../contract/src/managed/* automatically.
val syncContractAssets by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.dir("contract/src/managed"))
    into(layout.projectDirectory.dir("src/main/assets/managed"))
    // Strip the per-contract intermediate dir layer so the final
    // structure under assets is `managed/<contractName>/contract/index.js`
    // — matches what MidnightContract.fromAssets() expects.
    includeEmptyDirs = false
}

tasks.named("preBuild") {
    dependsOn(syncContractAssets)
}

dependencies {
    // Kuira SDK — one dep, full graph (Sigil identity, embedded wallet,
    // contract runtime, indexer, design system). See README "Pinned
    // versions" for the upgrade story.
    implementation(libs.kuira.dapp.ui)

    // Compose stack.
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Activity + Hilt + lifecycle plumbing.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
