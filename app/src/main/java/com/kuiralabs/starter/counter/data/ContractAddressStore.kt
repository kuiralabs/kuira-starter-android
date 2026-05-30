package com.kuiralabs.starter.counter.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.midnight.kuira.core.network.MidnightNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Per-network store of the deployed counter contract address.
// EncryptedSharedPreferences-backed because the address, while not
// secret on its own, is the binding between this user's sigil and a
// specific deployed contract — exfiltrating it could let a hostile
// app silently swap it for an attacker-controlled address before
// MidnightContract calls go out.
//
// Key shape: "counter.<network-name>" (counter.UNDEPLOYED, counter.PREPROD).
// Different networks get independent slots so switching network in the
// wallet panel doesn't strand the previous deploy.
@Singleton
class ContractAddressStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kuira-starter-counter-prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun keyFor(network: MidnightNetwork): String = "counter.${network.name}"

    fun get(network: MidnightNetwork): String? = prefs.getString(keyFor(network), null)

    fun put(network: MidnightNetwork, address: String) {
        prefs.edit().putString(keyFor(network), address).apply()
    }
}
