# Kuira Starter — Android

Minimum reproducible Kuira Android dApp. Sigil identity + embedded
wallet + a counter Compact contract — clone, set your `applicationId`
and `rpId`, host `assetlinks.json`, hit Run.

[![build](https://github.com/kuiralabs/kuira-starter-android/actions/workflows/build.yml/badge.svg)](https://github.com/kuiralabs/kuira-starter-android/actions/workflows/build.yml)

This repository is also a GitHub template — click **Use this template**
on the repo page to spin up your own dApp without forking.

---

## What this gives you

- **Identity** — `SigilStatusPanel` from the SDK. One biometric prompt
  forges a DID + wallet seed (passkey-PRF derived).
- **Wallet** — `WalletStatusPanel` from the SDK. NIGHT + DUST balance,
  receive-QR, dust registration.
- **Contract** — a 6-line counter in Compact (`contract/src/counter.compact`).
  Deploy on first run, increment with a single circuit call, read state
  via polling. Compiled artifacts are committed so the app builds
  out-of-box.

The whole demo is intentionally small (~250 LOC Kotlin code + ~6 lines
Compact, plus inline comments) so you can read every file in a single
sitting.

---

## Quick start

```bash
git clone https://github.com/kuiralabs/kuira-starter-android.git my-dapp
cd my-dapp
./gradlew :app:assembleDebug                # 5 to 7 minutes on a cold cache
```

Open in Android Studio. To actually run on a device, complete the four
**Before you run** items below.

---

## Before you run

The starter ships with four prep steps that you MUST complete before
the Sigil-forge path will succeed on a device. The first three are
single-string edits; the fourth is a terminal command:

| What | Where | Currently |
|---|---|---|
| `applicationId` + `namespace` | `app/build.gradle.kts` | `com.kuiralabs.starter.counter` |
| `PASSKEY_RP_ID` | `app/src/main/java/com/kuiralabs/starter/counter/di/PasskeyConfigModule.kt` | `"REPLACE_ME_WITH_YOUR_DOMAIN.example"` |
| `assetlinks.json` | hosted at `https://<your rpId>/.well-known/assetlinks.json` | not hosted |
| Wallet funding (localnet) | terminal — `mn` CLI | manual step, see below |

The `assetlinks.json` content must declare your app's signing
fingerprint. See the Kuira docs: [Add Kuira to an Android project §
Hosting assetlinks.json](https://kuiralabs.github.io/kuira-sdk-android/recipes/add-kuira-to-an-android-project/).

---

## Funding the embedded wallet

The Sigil-forge gives you a brand-new wallet with zero NIGHT and zero
DUST. Compact contracts need DUST to pay tx fees, and the SDK won't
deploy until both are present.

**On localnet (`MidnightNetwork.UNDEPLOYED`):**

1. Open the app, tap **Forge sigil** in the panel.
2. After forge, copy the wallet address from `WalletStatusPanel`.
3. In a terminal:
   ```bash
   mn airdrop 1000 --wallet <addr>      --network undeployed
   mn dust register --wallet <addr>     --network undeployed
   ```
4. Wait ~30 seconds for DUST to appear in the wallet panel.
5. Tap **Deploy counter**, then **Increment**.

**On PREPROD:** use the public faucet (link via the wallet panel's
copy-address button) instead of `mn airdrop`. DUST registration is
the same `mn dust register` command.

---

## Project layout

```
contract/                                 ← the on-chain piece
  src/counter.compact                       Compact source (6 lines + comments)
  src/managed/counter/                      compiled artifacts (committed)
  package.json                              pins compactc + runtime versions
  README.md                                 rebuild + verify recipe

app/                                      ← the Android app
  build.gradle.kts                          syncContractAssets Copy task
  src/main/java/.../
    KuiraStarterApp.kt                      @HiltAndroidApp
    MainActivity.kt                         AppCompatActivity + Compose
    di/PasskeyConfigModule.kt               REPLACE_ME rpId
    data/CounterContract.kt                 MidnightContract wrapper
    data/ContractAddressStore.kt            EncryptedSharedPreferences per network
    ui/CounterScreen.kt                     SigilPanel + WalletPanel + CounterCard
    ui/CounterCard.kt                       deploy + increment + count
    ui/CounterViewModel.kt                  state machine + 4s polling loop
    ui/CounterUiState.kt                    sealed interface
```

---

## Pinned versions

| Layer | Version |
|---|---|
| Kuira SDK | `0.1.0-alpha01` (Maven Central) |
| AGP | `8.13.2` |
| Kotlin | `2.3.20` |
| KSP | `2.3.6` |
| Hilt | `2.58` |
| Compose BOM | `2026.03.01` |
| JDK | `17` |
| `compactc` | `0.31.0` |
| Compact language pragma | `0.23.0` |
| `@midnight-ntwrk/compact-runtime` | `0.16.0` |

The Compact toolchain triple moves independently. See
[`contract/README.md`](contract/README.md) for the upgrade recipe.

---

## Known limitations today

These are gaps the SDK itself doesn't close at alpha01 — the starter
works around them visibly so consumers see the pattern and can swap
in the SDK-native path when it ships.

| Gap | Workaround in the starter | Closes when |
|---|---|---|
| **No `Flow<LedgerState>` for live contract state.** SDK alpha01 only exposes one-shot `MidnightContract.ledger().getUint64()`. | `CounterViewModel` runs a 4s polling loop while in the `Deployed` state. Single-line swap to `flow.collect` when a Flow API lands. | A Flow-based contract-state API ships in the SDK. |
| **Contract Gradle plugin (`com.midnight.kuira.contract`) not on Maven Central.** | `app/build.gradle.kts` has a hand-rolled `syncContractAssets` Copy task (same shape as the SDK docs' Recipe 3 alpha01 fallback). | SDK ships `com.midnight.kuira.contract` to Maven Central. |
| **No in-app airdrop / faucet button.** | Funding is a terminal step (`mn airdrop ... --network undeployed`). | The SDK ships an in-app airdrop helper for localnet, or upstream tooling subsumes the step. |
| **`androidx.security:security-crypto` is deprecated by Google industry-wide.** | Starter uses it for `ContractAddressStore` because the consensus migration target (Tink-backed DataStore) is still moving. Compile-time warnings are expected. | Google's recommended replacement stabilises. |
| **`SigilStatusPanel` defaults to a passkey rpId at compile time.** | Build will succeed with `REPLACE_ME_WITH_YOUR_DOMAIN.example`, but Forge will hit `RP_ID_MISMATCH` on a real device until the rpId points at a real domain whose `assetlinks.json` lists this app. | A preflight Gradle task catches this at build time. |

---

## FAQ

**Q: Forge fails with `RP_ID_MISMATCH`.**
A: Either `assetlinks.json` isn't reachable at
`https://<rpId>/.well-known/assetlinks.json`, or the SHA-256 fingerprint
in that file doesn't match this app's signing cert. The SDK docs'
[Recipe 1 § Hosting assetlinks.json](https://kuiralabs.github.io/kuira-sdk-android/recipes/add-kuira-to-an-android-project/)
has a copy-paste assetlinks.json with the right shape — replace the
fingerprint with your own (`./gradlew signingReport` to print it).

**Q: Deploy hangs at "Balancing".**
A: The wallet has zero DUST. Tap **Register dust** in the wallet panel,
or run `mn dust register --wallet <addr> --network undeployed`, then
wait ~30 seconds and retry deploy.

**Q: The count never updates after Increment.**
A: The polling loop fetches every 4 seconds; the tx itself takes one
block to land (~3s localnet, ~6s PREPROD). If the count is still stale
after 30s, check `adb logcat | grep -i counter` for indexer connection
errors — the indexer URL in `WalletConfig` may not be reachable.

**Q: Build fails with `Manifest merger failed: minSdkVersion 28 cannot
be smaller than version 30`.**
A: You've downgraded `minSdk` in `app/build.gradle.kts`. The SDK
requires minSdk 30 (Block Store + CredentialManager). Don't.

**Q: Build fails with `language version X.Y.Z mismatch`.**
A: You upgraded `compactc` or edited `pragma language_version` without
matching the other. See [`contract/README.md` § When `compactc` bumps](contract/README.md#when-compactc-bumps).

**Q: Sigil restore on a fresh device doesn't see the previous wallet.**
A: Block Store binds the backup to the Google Play Services account on
the device. If the second device is signed into a different Google
account, it will see `SigilStatus.None`, not `BackupAvailable`. Sign
into the same account or forge a new sigil on the second device.

---

## "Test of fire" log

The first cut of the Kuira SDK docs was tested by treating a
blank-context engineer as the consumer — building a starter from zero
using only the live website. Three doc bugs surfaced and were fixed
before this template was cut:

| Friction | Root cause | Where fixed |
|---|---|---|
| KSP version `2.3.20-2.0.4` not found | Recipe 1 quoted a version that doesn't exist on Maven Central. | Recipe 1 now shows `2.3.6` matching the SDK's actual pin (Fix #1 in the docs DevX punch-list). |
| `PasskeyConfig` unresolved import | Recipe 1 referenced `com.midnight.kuira.core.identity.PasskeyConfig` but the actual package is `…identity.passkey.PasskeyConfig`. | Recipe 1 import path corrected (Fix #1). |
| `SigilStatusPanel(activity = activity)` — no such parameter | Recipe 2 invented an API that doesn't exist on the real composable. | Recipe 2 now shows the actual signature (no `activity`; `MainActivity` extends `AppCompatActivity` so the panel finds a `FragmentActivity` host on its own). Fixes #1 and #5. |

This starter is, in effect, the canonical answer to "does the
documentation actually let a stranger build a working dApp." If you hit
a friction the docs don't anticipate, file an issue against
[kuiralabs/kuira-sdk-android](https://github.com/kuiralabs/kuira-sdk-android/issues)
— the cookbook is the source of truth for both humans and agents and
the right place to fix the documentation gap.

---

## Roadmap

What's missing in the starter today is missing because the SDK doesn't
yet expose it. As the SDK closes each gap, the starter absorbs the new
API at the next pin bump.

- **Flow-based contract state** — would replace the 4s polling loop
  with a push-based subscription.
- **Contract Gradle plugin on Maven Central** — would replace the
  hand-rolled `syncContractAssets` task with a single plugin id.
- **Preflight Gradle task** — would catch placeholder rpId,
  unreachable `assetlinks.json`, and Compact runtime mismatches at
  build time instead of as runtime exceptions.
- **Localnet in-app airdrop** — `BuildConfig.DEBUG`-gated fund button
  so the starter doesn't have to send users to a terminal.

Track these and other gaps at
[kuiralabs/kuira-sdk-android/issues](https://github.com/kuiralabs/kuira-sdk-android/issues).

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
