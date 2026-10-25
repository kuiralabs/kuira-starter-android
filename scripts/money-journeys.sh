#!/usr/bin/env bash
#
# money-journeys.sh — the chain half of the wallet journeys that move NIGHT
# (journeys/05-receive-night.xml and journeys/06-send-night.xml).
#
#   scripts/money-journeys.sh prepare
#   scripts/money-journeys.sh verify <whole-NIGHT-expected>
#
# WHY A SCRIPT AND NOT JOURNEY STEPS. A journey drives the UI; it cannot fund a wallet, and it
# cannot read the chain. Receiving needs an outside party to send, and a send is only proven by the
# RECIPIENT's balance moving — the app reporting "Sent" is the app's claim, not the chain's. So the
# counterparty and the oracle live here, and the journeys assert only what the screen says.
#
# WHY LOCALNET. `mn` is a complete counterparty on exactly one network. `mn airdrop` mints from the
# genesis wallet, so funding is one command; the public testnets have no faucet, and a wallet funded
# there cannot register for dust until its NIGHT coin has aged, so it cannot send for an hour.
# Localnet also costs nothing and resets freely.
#
# PREPARE prints the three facts the journeys need, then leaves the chain ready:
#   - the app wallet's address, funded to at least MIN_NIGHT (what the receive journey observes)
#   - the recipient's address, to type into the send wizard
#   - the recipient's balance before the send, so VERIFY can tell what moved
#
# Prereqs: a ledger-8 localnet answering on the pinned endpoints, Docker running, `mn` on PATH, and
# kuira.dev.seed in local.properties — the seed this app opens its wallet with.
set -uo pipefail
cd "$(dirname "$0")/.."

# PINNED, not bare `mn`: the CLI is ledger-versioned (0.5.x drives ledger 8, 0.6.x ledger 9) and
# refuses a mismatched chain rather than mis-signing. All three endpoints are pinned too — with more
# than one local stack running, `mn` refuses to guess which chain to use, and with one it can still
# resolve a different chain than the device reads.
MN_CLI="${MN_CLI:-npx -y midnight-wallet-cli@0.5.8}"
IFS=' ' read -r -a MN <<< "$MN_CLI"
MN_NODE="${MN_NODE:-ws://127.0.0.1:9944}"
MN_INDEXER_WS="${MN_INDEXER_WS:-ws://127.0.0.1:8088/api/v4/graphql/ws}"
MN_PROOF_SERVER="${MN_PROOF_SERVER:-http://127.0.0.1:6300}"
EP=(--node "$MN_NODE" --indexer-ws "$MN_INDEXER_WS" --proof-server "$MN_PROOF_SERVER")

# The send journey spends from this; the receive journey watches it arrive.
# Address derivation is the ONE call that cannot use the pinned CLI. 0.5.x accepts only a 64-char
# seed (32-byte raw), and the SDK's dev hatch accepts only the 128-char form (the 64-byte BIP-39
# seed), so the ledger-8 build rejects the very seed the app opens its wallet with. Deriving an
# address is pure key derivation — no chain, no ledger version — so a 0.6.x build answers it
# correctly for a ledger-8 chain.
MN_DERIVE_CLI="${MN_DERIVE_CLI:-mn}"
IFS=' ' read -r -a MN_DERIVE <<< "$MN_DERIVE_CLI"

MIN_NIGHT="${MIN_NIGHT:-100}"
AIRDROP_NIGHT="${AIRDROP_NIGHT:-10000}"
# A second wallet the operator controls, so the journeys' NIGHT ping-pongs and never leaves.
RECIPIENT_WALLET="${RECIPIENT_WALLET:-bob}"
STATE_FILE="${TMPDIR:-/tmp}/kuira-money-journeys.env"

ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; }
die()  { printf '  \033[31m✗ %s\033[0m\n' "$1" >&2; exit 1; }
bold() { printf '\033[1m==> %s\033[0m\n' "$1"; }

prop() { awk -F= -v k="$1" '$1==k{sub(/^[^=]*=/,""); print; exit}' local.properties; }

# Whole NIGHT held at an address, read from the machine-readable form. The pretty box goes to
# stderr; only --json puts the figure on stdout, and it is absent entirely for an empty wallet.
night_at() {
    local raw
    raw="$("${MN[@]}" balance "$1" "${EP[@]}" --json 2>/dev/null \
        | tr '\r' '\n' | grep -oE '"NIGHT":"[0-9.]+"' | head -1 | sed 's/.*:"//;s/"//')"
    printf '%s' "${raw%%.*}"
}

localnet_ledger() {
    curl -sf -m 5 -X POST "${MN_NODE/ws:/http:}" -H 'Content-Type: application/json' \
        -d '{"jsonrpc":"2.0","id":1,"method":"midnight_ledgerVersion","params":[]}' 2>/dev/null \
        | sed -n 's/.*"result": *"\([^"]*\)".*/\1/p'
}

prepare() {
    echo
    bold "money journeys — chain prep on $MN_NODE"

    LEDGER="$(localnet_ledger)"
    [ -n "$LEDGER" ] || die "no localnet answering at $MN_NODE. Bring one up with 'mn localnet up'."
    ok "localnet on ledger $LEDGER"

    SEED="$(prop kuira.dev.seed)"
    [ -n "$SEED" ] || die "kuira.dev.seed is missing from local.properties — that seed IS the wallet the app opens, so without it the journeys would watch a different wallet than the one on screen."

    APP="$("${MN_DERIVE[@]}" address --seed "$SEED" --network undeployed 2>/dev/null | head -1 | tr -d '[:space:]')"
    case "$APP" in
        mn_addr_undeployed1*) ok "app wallet $APP" ;;
        *) die "could not derive the app wallet's localnet address from kuira.dev.seed via '$MN_DERIVE_CLI address'. That CLI must accept a 128-char seed — 0.5.x takes only 64." ;;
    esac

    HELD="$(night_at "$APP")"
    HELD="${HELD:-0}"
    if [ "$HELD" -lt "$MIN_NIGHT" ]; then
        echo "  holds ${HELD} NIGHT — airdropping ${AIRDROP_NIGHT}…"
        "${MN[@]}" airdrop "$AIRDROP_NIGHT" --wallet "$APP" "${EP[@]}" >/dev/null 2>&1 \
            || die "'mn airdrop' failed. With a localnet up this is usually the CLI/chain ledger pairing — 0.5.x drives ledger 8, 0.6.x ledger 9; this chain reports $LEDGER."
        HELD="$(night_at "$APP")"
        HELD="${HELD:-0}"
        [ "$HELD" -ge "$MIN_NIGHT" ] \
            || die "airdrop reported success but the address still holds ${HELD} NIGHT."
    fi
    ok "app wallet holds $HELD NIGHT"

    RECIPIENT="$(python3 -I -c "
import json,os,sys
p=os.path.expanduser('~/.midnight/wallets/$RECIPIENT_WALLET.json')
if not os.path.exists(p): sys.exit(1)
print((json.load(open(p)).get('addresses') or {}).get('undeployed',''))
" 2>/dev/null)"
    case "$RECIPIENT" in
        mn_addr_undeployed1*) ok "recipient '$RECIPIENT_WALLET' $RECIPIENT" ;;
        *) die "no localnet address for the '$RECIPIENT_WALLET' wallet. Create one: mn wallet generate $RECIPIENT_WALLET --network undeployed" ;;
    esac
    [ "$RECIPIENT" != "$APP" ] || die "the recipient resolved to the app's own wallet; the send would prove nothing."

    BEFORE="$(night_at "$RECIPIENT")"
    BEFORE="${BEFORE:-0}"
    ok "recipient holds $BEFORE NIGHT before the send"

    { echo "RECIPIENT=$RECIPIENT"; echo "BEFORE=$BEFORE"; } > "$STATE_FILE"

    echo
    bold "what the journeys need"
    echo "  app wallet (the receive journey watches this):"
    echo "      $APP"
    echo "  recipient (type this into the send wizard's TO field):"
    echo "      $RECIPIENT"
    echo "  recipient balance before: $BEFORE NIGHT"
    echo
    echo "  After the send journey reports Sent:  scripts/money-journeys.sh verify <amount>"
}

verify() {
    local expected="${1:?usage: money-journeys.sh verify <whole-NIGHT-expected>}"
    [ -f "$STATE_FILE" ] || die "no prepared state at $STATE_FILE — run 'prepare' before the journeys."
    # shellcheck disable=SC1090
    . "$STATE_FILE"

    echo
    bold "the chain must show the send landed"
    local after moved
    after="$(night_at "$RECIPIENT")"
    after="${after:-0}"
    moved=$(( after - BEFORE ))
    [ "$moved" -ge "$expected" ] \
        || die "the app reported a send but the chain moved ${moved} NIGHT to the recipient (expected >= ${expected}). A submitted transaction can finalize late — re-read with: $MN_CLI balance $RECIPIENT ${EP[*]}"
    ok "chain confirms $moved NIGHT reached $RECIPIENT"
    echo
    printf '\033[32m════ MONEY JOURNEYS GREEN — the chain moved NIGHT to the recipient ════\033[0m\n'
}

case "${1:-}" in
    prepare) prepare ;;
    verify)  shift; verify "$@" ;;
    *) die "usage: money-journeys.sh prepare | verify <whole-NIGHT-expected>" ;;
esac
