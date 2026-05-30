# Kuira Starter — Android

Minimum reproducible Kuira Android dApp. Sigil identity + embedded
wallet + a counter Compact contract — clone, set your `applicationId`
and `rpId`, hit Run.

> **Status:** under construction. Phase 2 of 6 complete — the Compact
> contract is written, compiled, and end-to-end verified against
> localnet. The Android app is being built next. Track progress
> against the [project plan](https://github.com/kuiralabs/kuira-sdk-android).

## What's here today

- **`contract/`** — the on-chain piece. A 6-line counter contract in
  Compact, compiled artifacts committed for out-of-box build, plus a
  step-by-step rebuild + verify recipe. See [`contract/README.md`](contract/README.md).

## What's coming

- **`app/`** — the Android app: Sigil identity, embedded wallet,
  contract deploy/call wiring, state polling.
- **`.github/workflows/build.yml`** — CI running `assembleDebug` on
  every PR.
- This README will grow into a one-page quick-start.

## Pinned versions (today)

| Layer | Version |
|---|---|
| Kuira SDK | `0.1.0-alpha01` |
| `compactc` | `0.31.0` |
| Compact language | `0.23.0` |
| `@midnight-ntwrk/compact-runtime` | `0.16.0` |

The Compact toolchain triple moves independently — see
[`contract/README.md`](contract/README.md) for the version map.

## License

Apache 2.0 — see [LICENSE](LICENSE).
