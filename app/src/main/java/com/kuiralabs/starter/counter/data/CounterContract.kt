package com.kuiralabs.starter.counter.data

import android.content.Context
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.sdk.MidnightSdk

// Thin wrapper around MidnightContract for the counter — loads assets,
// holds wiring constants, gives the ViewModel a tight three-method
// surface (deploy / increment / readCount).
//
// "Counter" is the per-contract name under app/src/main/assets/managed/
// (which the syncContractAssets Gradle task populates from
// ../contract/src/managed/counter/). Changing this requires changing
// counter.compact's filename + the Copy task source path.
internal object CounterContract {

    private const val NAME = "counter"
    private const val CIRCUIT_INCREMENT = "increment"
    private const val LEDGER_FIELD_COUNT = "count"

    private fun assetPath(suffix: String): String = "managed/$NAME/$suffix"

    private fun loadVerifierKeys(context: Context): Map<String, ByteArray> {
        // The deploy path requires every circuit's verifier key bytes so
        // they can be embedded in the on-chain contract artifact. Call
        // path does not need them — fetched fresh from chain.
        val verifierBytes = context.assets
            .open(assetPath("keys/$CIRCUIT_INCREMENT.verifier"))
            .use { it.readBytes() }
        return mapOf(CIRCUIT_INCREMENT to verifierBytes)
    }

    private fun buildHandle(
        context: Context,
        sdk: MidnightSdk,
        address: String?,
        forWrite: Boolean,
    ): MidnightContract = MidnightContract.create(sdk.config) {
        name = NAME
        contractJs = context.assets.open(assetPath("contract/index.js"))
        if (address != null) this.address = address
        if (forWrite) {
            coinPublicKey = sdk.coinPublicKey
            circuitVerifierKeys = loadVerifierKeys(context)
        }
    }

    suspend fun deploy(context: Context, sdk: MidnightSdk): String {
        val handle = buildHandle(context, sdk, address = null, forWrite = true)
        return handle.deploy().contractAddress
    }

    suspend fun increment(context: Context, sdk: MidnightSdk, address: String) {
        val handle = buildHandle(context, sdk, address = address, forWrite = true)
        handle.call(CIRCUIT_INCREMENT)
    }

    // Read-only handle: no cpk, no verifier keys. The polling loop
    // wants a single one of these for the lifetime of a deployed
    // address — re-creating per tick reopens the contract JS stream
    // every 4 seconds for no win.
    fun buildReadHandle(context: Context, sdk: MidnightSdk, address: String): MidnightContract =
        buildHandle(context, sdk, address = address, forWrite = false)

    suspend fun readCount(handle: MidnightContract): Long =
        handle.ledger().getUint64(LEDGER_FIELD_COUNT)
}
