# Counter contract

Universal "Hello World" of smart contracts, in Compact:

```compact
export ledger count: Counter;

export circuit increment(): [] {
    count.increment(1);
}
```

## Toolchain (pinned)

| Layer | Version |
|---|---|
| `compactc` (compiler) | **0.31.0** |
| Language (`pragma language_version`) | **0.23.0** |
| `@midnight-ntwrk/compact-runtime` | **0.16.0** |

Mismatched versions surface as `language version X.Y.Z mismatch` at
compile time. The three numbers are intentionally independent — the
compiler binary, the source language, and the JS runtime each version
on their own cadence.

## Build the artifacts

The Android app consumes `src/managed/counter/` — `index.js` (contract
class), `keys/increment.prover`, `keys/increment.verifier`. These are
committed to the repo so the app builds out of the box; rebuild only
if you edit `counter.compact`.

```bash
# direct compactc invocation
~/.compact/versions/0.31.0/aarch64-darwin/compactc \
  src/counter.compact src/managed/counter

# or use the iterative dev loop (watch + auto-deploy)
mn dev .
```

## Verify against a localnet

```bash
mn localnet up                                      # if not running
mn airdrop 1000 --wallet dev-alice                  # fund a wallet
mn dust register --wallet dev-alice --network undeployed
mn contract deploy --managed src/managed/counter \
    --wallet dev-alice --network undeployed         # → address

mn contract state --address <addr> \
    --managed src/managed/counter \
    --wallet dev-alice --network undeployed         # count: 0

mn contract call --address <addr> --circuit increment \
    --managed src/managed/counter \
    --wallet dev-alice --network undeployed         # ✓

mn contract state --address <addr> \
    --managed src/managed/counter \
    --wallet dev-alice --network undeployed         # count: 1
```

## When `compactc` bumps

The committed artifacts go stale. To upgrade:

1. Install the new compactc into `~/.compact/versions/<new>/`.
2. Bump `engines.compactc` in `package.json`.
3. Bump `@midnight-ntwrk/compact-runtime` to match (run
   `compactc --runtime-version` to confirm).
4. Bump `pragma language_version` in `counter.compact` (run
   `compactc --language-version` to confirm).
5. `rm -rf src/managed/counter && compactc src/counter.compact src/managed/counter`.
6. Re-verify with the localnet flow above.
7. Commit the regenerated artifacts.
