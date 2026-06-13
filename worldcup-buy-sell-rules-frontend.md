# Frontend plan — buy/sell rules (World Cup mode)

Backend work for these three rules is **done and merged on the backend** (transaction flow +
tests). This doc is the handoff so the frontend can surface them. All three only apply while the
app is in **World Cup mode** (the active pricing strategy); in club mode the transaction API
behaves exactly as before.

## The three rules

1. **Squad cap = 4.** A user can own at most 4 players at a time. A 5th buy is rejected.
2. **No buying a player who is in a live match.** While a World Cup player's match is being
   played, his price is mid-swing, so he can't be bought until the match is over.
3. **48h sell lock.** A player can't be sold until 48 hours after he was bought. Prevents
   buying right before a match and dumping right after to harvest the in-match price spike.

---

## API contract

### Transaction endpoint (unchanged shape)

`POST /api/transactions`

Request body:
```json
{ "playerId": 123, "playerSofascoreId": 818244, "transactionType": 1, "price": 1000 }
```
`transactionType`: `1` = buy, `2` = sell.

**Success** → HTTP 200:
```json
{ "status": 0, "transactionId": 55, "timestamp": "2026-06-13T17:46:10.3", "error": null }
```

**Rejected** → HTTP 400:
```json
{ "status": -1, "error": { "errorType": "sell_locked", "errorMsg": "a player can only be sold 48 hours after buying him", "playerPrice": null } }
```

### `error.errorType` values to branch on

`errorType` is the stable identifier — **switch on it, do not match `errorMsg` text** (`errorMsg`
is human-readable and may change). Existing code that keys off `"price"` keeps working.

| errorType             | Meaning                                   | Suggested UI                                                            |
|-----------------------|-------------------------------------------|------------------------------------------------------------------------|
| `price`               | Price drifted; `error.playerPrice` is the new live price | Re-show the player with the new price, ask to confirm (existing behavior) |
| `max_buy`             | Squad already has 4 players               | "Your squad is full (max 4 players). Sell one before buying another."   |
| `live_match`          | Player is currently playing a match       | "This player is in a live match right now — you can buy him after it ends." |
| `sell_locked`         | Selling inside the 48h lock window        | "You can only sell a player 48 hours after buying him." (+ time left, see below) |
| `already_owned`       | Buying a player already owned             | existing handling                                                       |
| `not_owned`           | Selling a player not owned                | existing handling                                                       |
| `insufficient_points` | Not enough points to buy                  | existing handling                                                       |

> These are the source of truth. Even if the UI hides/disables a button, always handle the
> error too (another tab, a match that just kicked off, clock skew, etc.).

### My Squad endpoint — now returns `boughtAt`

`GET /api/users/myteam` → each player object now includes a new field:

```json
{ "id": 123, "name": "...", "buyPrice": 1000, "price": 1200, "boughtAt": "2026-06-13T17:46:10.3", ... }
```

- `boughtAt` is the buy timestamp (server clock, Europe/Istanbul). `null` for older holdings
  bought before this rule existed — treat `null` as "sellable now" (no lock).
- **Sellable-at** = `boughtAt + 48h`. Use it to drive the countdown / disabled sell button.

---

## Frontend tasks

### 1. Buy modal — disclaimer (rule 3)
On the buy confirmation UI, add a always-visible note:
> "Heads up: once bought, a player can't be sold for 48 hours."

Optionally also note the squad cap there ("Squad: 3 / 4").

### 2. Buy — squad cap (rule 1)
- Proactive: if the user already owns 4, disable the buy button and show "Squad full (4/4)".
- Reactive: on `errorType === "max_buy"`, show the squad-full message.

### 3. Buy — live match (rule 2)
- This is server-driven (depends on live fixtures), so handle it **reactively**: on
  `errorType === "live_match"`, show "This player is in a live match — buy him after it ends."
- (Optional polish) if the player card already shows a "LIVE" badge from the fixtures feed,
  you can pre-disable buy, but the error is what guarantees correctness.

### 4. Sell — 48h lock (rule 3)
- Proactive: in My Squad, compute `sellableAt = boughtAt + 48h`.
  - If `boughtAt == null` or `now >= sellableAt` → sell enabled.
  - Else → disable the sell button and show remaining time, e.g. "Sellable in 9h 12m".
- Reactive: on `errorType === "sell_locked"`, show "You can only sell a player 48 hours after
  buying him."

### Suggested helper

```js
const SELL_LOCK_HOURS = 48; // mirrors backend TransactionService.SELL_LOCK_HOURS

function sellState(boughtAt) {
  if (!boughtAt) return { sellable: true };
  const unlock = new Date(boughtAt).getTime() + SELL_LOCK_HOURS * 3600 * 1000;
  const msLeft = unlock - Date.now();
  return msLeft <= 0 ? { sellable: true } : { sellable: false, msLeft };
}
```

---

## Notes / edge cases
- All three rules are enforced only in World Cup mode; no frontend gating needed beyond what
  you already do for WC vs club UI.
- `boughtAt` has no timezone suffix (it's a `LocalDateTime`); it's the server's wall clock
  (Europe/Istanbul). The countdown is cosmetic — the backend error is authoritative, so minor
  clock skew only affects the displayed "time left", never whether a sell actually goes through.
- The squad cap (4) and lock window (48h) are backend constants. If they ever change, this doc
  and the `SELL_LOCK_HOURS` helper need updating.
