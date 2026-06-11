package com.kuiralabs.starter.counter.data

import android.content.Context
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.sdk.MidnightSdk

// Thin wrapper around MidnightContract for the counter — loads assets,
// holds wiring constants, gives the ViewModel a tight surface
// (deploy / increment / readCount).
//
// The io.github.kuiralabs.contract Gradle plugin syncs the compiled
// contract into the app's assets in the SDK's canonical layout: the
// runtime JS as runtime/counter-contract.js and the circuit keys as
// keys/increment.{prover,verifier,bzkir}. Changing NAME requires
// changing the kuiraContract { source } path in build.gradle.kts.
internal object CounterContract {

    private const val NAME = "counter"
    private const val CIRCUIT_INCREMENT = "increment"
    private const val LEDGER_FIELD_COUNT = "count"

    private const val CONTRACT_JS_ASSET = "runtime/$NAME-contract.js"
    private const val VERIFIER_ASSET = "keys/$CIRCUIT_INCREMENT.verifier"

    private fun loadVerifierKeys(context: Context): Map<String, ByteArray> {
        // The deploy path embeds every circuit's verifier key in the
        // on-chain contract artifact. Calls don't need it — it's
        // fetched fresh from chain.
        val verifierBytes = context.assets
            .open(VERIFIER_ASSET)
            .use { it.readBytes() }
        return mapOf(CIRCUIT_INCREMENT to verifierBytes)
    }

    // Stage the contract's proving keys from assets/keys into the SDK's
    // keysDir so the local prover finds increment.prover. Idempotent —
    // gated internally on an APK stamp + per-file presence check.
    private fun installProvingKeys(context: Context) {
        ProvingKeyManager(context).installCircuitKeysFromAssets()
    }

    private fun buildHandle(
        context: Context,
        sdk: MidnightSdk,
        address: String?,
        forWrite: Boolean,
    ): MidnightContract = MidnightContract.create(sdk.config) {
        name = NAME
        contractJs = context.assets.open(CONTRACT_JS_ASSET)
        if (address != null) this.address = address
        if (forWrite) {
            coinPublicKey = sdk.coinPublicKey
            circuitVerifierKeys = loadVerifierKeys(context)
        }
    }

    suspend fun deploy(
        context: Context,
        sdk: MidnightSdk,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ): String {
        installProvingKeys(context)
        val handle = buildHandle(context, sdk, address = null, forWrite = true)
        return handle.deploy(onProgress = onProgress).contractAddress
    }

    suspend fun increment(
        context: Context,
        sdk: MidnightSdk,
        address: String,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        installProvingKeys(context)
        val handle = buildHandle(context, sdk, address = address, forWrite = true)
        handle.call(CIRCUIT_INCREMENT, onProgress = onProgress)
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
