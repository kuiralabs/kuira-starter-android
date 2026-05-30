package com.kuiralabs.starter.counter.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.midnight.kuira.core.network.MidnightNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Per-network store of the deployed counter contract address.
// EncryptedSharedPreferences-backed to stay consistent with the SDK's
// own storage pattern (auth tokens, sigil state) and to treat the
// user→contract binding as a sensitive default. The address is not a
// secret on its own — it's public on chain — but encrypting at rest
// avoids it showing up plaintext in backup snapshots or post-mortem
// device dumps.
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
