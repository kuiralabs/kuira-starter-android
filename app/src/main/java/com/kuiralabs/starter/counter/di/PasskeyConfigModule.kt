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

    // Local-dev rpId so the existing forged sigil on emulator-5554
    // continues to sign-in against the same passkey. Do NOT commit
    // this change to the template — revert to REPLACE_ME before
    // pushing. The Kuira-org assetlinks.json at
    // https://kuiralabs.github.io/.well-known/assetlinks.json
    // already lists com.kuiralabs.starter.counter as a target.
    private const val PASSKEY_RP_ID = "kuiralabs.github.io"

    @Provides
    @Singleton
    fun providePasskeyConfig(): PasskeyConfig =
        PasskeyConfig(rpId = PASSKEY_RP_ID, rpName = "Kuira Starter")
}
