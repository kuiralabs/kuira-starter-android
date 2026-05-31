package com.kuiralabs.starter.counter.di

import com.midnight.kuira.core.identity.passkey.PasskeyConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// The SDK requires the consumer to bind one PasskeyConfig — it does
// not (and cannot) know your app's domain. Omitting this module is a
// fail-fast Dagger missing-binding error at build time, which is the
// intended "declare your domain" signal.
@Module
@InstallIn(SingletonComponent::class)
object PasskeyConfigModule {

    // ─── BEFORE YOU RUN ────────────────────────────────────────────
    // Replace REPLACE_ME with the domain you control and host
    // .well-known/assetlinks.json on. Forge will fail with
    // RP_ID_MISMATCH until this points at a real, reachable domain
    // whose assetlinks.json lists this app's package + signing cert.
    //
    // See: https://kuiralabs.github.io/kuira-sdk-android/recipes/bind-your-app-to-a-passkey-domain/
    private const val PASSKEY_RP_ID = "REPLACE_ME_WITH_YOUR_DOMAIN.example"

    @Provides
    @Singleton
    fun providePasskeyConfig(): PasskeyConfig =
        PasskeyConfig(rpId = PASSKEY_RP_ID, rpName = "Kuira Starter")
}
