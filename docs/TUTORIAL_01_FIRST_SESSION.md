# Tutorial 01 — Your First Session

The full happy path: install the pair, start a session, search for the crystal count, narrow through the mirrors, write a value, undo it, freeze it, and stop cleanly. Budget 15 minutes.

Each step follows the same shape — **Action / You should see / If not**. Recovery advice is inline; broader issues live in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Prerequisites

- Both APKs built from the same machine (paired debug builds share the debug keystore): [QUICKSTART.md](QUICKSTART.md) steps 1–4.
- A device or emulator on Android 6.0 (API 23)+. Not yet validated on physical hardware — expectations here describe intended behavior.
- No root, no special setup. Ever.

## Step 1 — Start a session in Space

- **Action**: Open AetherPlay Space. On the default space ("Main space"), start a session.
- **You should see**: An ongoing notification appears (on Android 13+ you are first asked for notification permission). The notification carries *Open game* and *Stop session* actions.
- **If not**: If you denied notifications, the session still runs — you just have no visible notification, so start the game from Space's Library instead. If Space refuses to start a session, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Step 2 — Open Crystal Sprint and catch 5 crystals

- **Action**: Use the notification's *Open game* action (or open it from Space's Library). Catch exactly 5 crystals, then pause playing.
- **You should see**: The game HUD's crystal counter reads 5.
- **If not**: The count does not matter, only that you know it. Use whatever number you actually have and substitute it in the steps below.

## Step 3 — Connect Lab

- **Action**: Open AetherPlay Lab and connect.
- **You should see**: Lab reports it is connected to the running session.
- **If not**: Lab can only attach to an already-running session — it cannot start one, by design. Confirm Space's session is still running. "Space is not connected" and signature-mismatch causes are covered in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Step 4 — Search for the crystal count

- **Action**: In the search composer, set type **Int32**, mode **exact**, value **5**, alignment **4**, regions both `state` and `playfield`. Run the search.
- **You should see**: A result list with **several** addresses, all currently holding 5. This is expected — do not be alarmed.
- **If not**: Zero results usually means the count changed while you typed (energy drains, decoys drift). Re-check the HUD and re-run with the current count, or switch to an unknown search as taught in [TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md](TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md).

## Why several results? The mirrors

The training game deliberately shows you a realistic search problem:

- `0x7B000000` (region `state`) — the **real** crystals field. Int32 at offset +0.
- `0x7B100078` (region `playfield`) — a **mirror** of the crystal count, kept in the read-only telemetry region.
- Plus **lagging display copies** — values that track the crystal count but update late.

All of them hold 5 right now. The next steps separate them: when the count changes, the real field and the mirror move promptly, while lagging copies move late or drift — and refinement makes them fall away.

## Step 5 — Catch one more crystal (now 6)

- **Action**: Return to Crystal Sprint and catch one more crystal.
- **You should see**: The HUD reads 6.
- **If not**: If you accidentally caught several, use the real number in the next two steps.

## Step 6 — Refine: Increased

- **Action**: Back in Lab, refine the result set with **Increased**.
- **You should see**: The list shrinks. Every address whose value did not go up is gone.
- **If not**: If the list emptied, the count changed twice (some copy lagged). Redo the search from Step 4 with the current count.

## Step 7 — Refine: Exact 6

- **Action**: Refine with **Exact value** = 6.
- **You should see**: A short list — typically the real field `0x7B000000` and its mirror `0x7B100078` plus at most a few up-to-date copies.
- **If not**: Still too many? Play a little (catch or spend), then alternate *Changed* and *Unchanged* refinements — decoys drift on their own, so they expose themselves.

## Step 8 — Open the detail sheet

- **Action**: Tap the result at `0x7B000000`.
- **You should see**: The detail sheet with the address, its Int32 type, and a live read of the value 6. The write controls offer a preview step, not an immediate write.
- **If not**: If you tapped the mirror at `0x7B100078` instead, its region is `playfield` and writes to it are rejected as read-only — that is intended. Go back and pick `0x7B000000`.

## Step 9 — Write 99: Preview, then Commit

- **Action**: Enter **99** and choose **Preview**.
- **You should see**: A preview showing old to new (`6 -> 99`) for the address. Nothing is written yet.
- **Action**: Choose **Commit**.
- **You should see**: A per-item success outcome, and the game HUD now shows 99.
- **If not**: If commit reports failure, the value changed between preview and commit (the game re-validates at commit time) — re-preview and commit again. If the sheet says the address is read-only, you are on a `playfield` address; use `0x7B000000`.

## Step 10 — Undo

- **Action**: Go to Lab's History tab and undo.
- **You should see**: The journal's last entry is your write of 99. Undo restores the previous value, and the HUD returns to 6.
- **If not**: Undo restores the **last write only**, and only if the current value still equals what was written (identity re-check). If the game changed the field after your write, or the session generation changed, undo refuses with an explanation. That is a safety feature, not a bug — see [TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md](TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md) for undo semantics.

## Step 11 — Write 99 again and freeze it

- **Action**: From the detail sheet, write 99 again (preview, commit). Then freeze the address.
- **You should see**: The freeze appears in the History tab's freeze list. Catch a few more crystals — the HUD keeps snapping back to 99.
- **If not**: A freeze re-applies its value every 300 ms, so the HUD may flicker to the caught count for a moment before returning to 99. If the value never returns to 99, confirm the session is still running — all freezes stop when the session stops.

## Step 12 — Stop the session

- **Action**: In Space (or from the session notification), choose *Stop session*.
- **You should see**: The notification goes away, the game closes, and Lab reports the session has ended.
- **If not**: If the notification is missing (permission denied), stop the session from Space's Session screen.

## Step 13 — Verify the freezes stopped

- **Action**: Look at Lab's History tab.
- **You should see**: The freeze list is cleared. Every freeze stops when the session stops, without exception.
- **If not**: If a freeze still shows as active, it belongs to no live session — use *Stop all*.

## Reset and exit path

- To replay from scratch: reset the run inside Crystal Sprint. Note this bumps the session generation — Lab operations issued before the reset are rejected with a "session changed" message, which is intended.
- To leave: stop the session (Step 12) and simply leave both apps. Nothing runs in the background once the session is stopped, and no data leaves the device.

## Where to go next

- [TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md](TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md) — unknown-value searches (energy, Float32) and undo semantics in depth.
- [TUTORIAL_03_SPACES_AND_SAVE_DATA.md](TUTORIAL_03_SPACES_AND_SAVE_DATA.md) — named spaces and what persists.
- [USER_GUIDE.md](USER_GUIDE.md) — the complete screen reference.
