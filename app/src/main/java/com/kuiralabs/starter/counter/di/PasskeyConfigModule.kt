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

    // REPLACE_ME — set this to YOUR passkey domain when you make the app
    // yours: the host serving your `.well-known/assetlinks.json`, which must
    // list your applicationId + signing SHA-256. This default points at the
    // Kuira org domain, which works out of the box for the starter's default
    // applicationId (com.kuiralabs.starter.counter) so you can run it
    // immediately — change both together. See the README and the "Bind your
    // app to a passkey domain" recipe.
    private const val PASSKEY_RP_ID = "kuiralabs.github.io"

    @Provides
    @Singleton
    fun providePasskeyConfig(): PasskeyConfig =
        PasskeyConfig(rpId = PASSKEY_RP_ID, rpName = "Kuira Starter")
}
