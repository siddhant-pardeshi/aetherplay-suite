# Tutorial 03 — Spaces and Save Data

Named spaces are AetherPlay's way of keeping multiple independent Crystal Sprint saves on one device: one space, one save. This tutorial covers creating and renaming spaces, what persists (and what does not), where it lives, what deletion really deletes, and what a run reset does to a live Lab session.

Prerequisites: a paired install ([QUICKSTART.md](QUICKSTART.md)) and ideally one run of [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md).

## Step 1 — Look at your spaces

- **Action**: Open AetherPlay Space and go to the Spaces screen.
- **You should see**: At least the default space ("Main space"), with per-space storage usage shown.
- **If not**: If the list is empty, create a space (Step 2) — but the default space should exist on a fresh install.

## Step 2 — Create a second space

- **Action**: Create a new space and name it, for example, "Lab practice".
- **You should see**: The new space in the list, starting with its own fresh save data and its own storage usage entry.
- **If not**: Space names have to be unique to be useful, but the screen will tell you if a create fails; retry with a different name.

## Step 3 — Play two different saves

- **Action**: Start a session on "Main space", open Crystal Sprint, and note your crystal count. Stop the session. Then start a session on "Lab practice" and open the game.
- **You should see**: The "Lab practice" run starts from that space's own save — a fresh crystal count, energy, best, gems, tokens, and run time — not the numbers you just built on "Main space".
- **If not**: If the numbers carried over, confirm you started the second session on the intended space; each session plays the save of the space it was started on.

## What persists, and what does not

Crystal Sprint persists these fields per space:

| Field | Type | Offset in `state` |
| --- | --- | --- |
| crystals | Int32 | +0 |
| energy | Float32 (clamped 0..100) | +4 |
| best | Int64 | +8 |
| speed | Float32 (clamped 0.25..4) | +16 |
| gems | Int32 | +20 |
| tokens | Int32 | +24 |
| runTime | Float32 | +28 |

Falling crystals — the ones currently dropping in the playfield — do **not** persist. A stopped and resumed session resumes from the saved field values, not from a mid-air snapshot of the run.

## Where the saves live

Save data and space metadata are stored as files in Space's **app-private storage** — no shared storage, no external storage, no cloud. Each space's save is independent, and the storage usage shown on the Spaces screen reflects these files. Nothing outside Space (or Android's per-app sandbox on your behalf, for example during backup or uninstall) can read them.

## Step 4 — Rename a space

- **Action**: Rename "Lab practice" to something else.
- **You should see**: The display name changes; the save data is untouched. Renaming never affects saves.
- **If not**: Renaming is metadata-only — if the rename did not take, retry; nothing was lost either way.

## Step 5 — Delete a space (read the warning)

- **Action**: Delete the space you renamed in Step 4.
- **You should see**: A confirmation warning before anything happens. Confirm only when you mean it.
- **You should see** (after confirming): The space and its save data are gone from the list and from storage.
- **If not**: If you cancelled the confirmation, nothing was deleted — cancellation is always safe.

Deletion is permanent for that space's save files. There is no recycle bin, no undo, and no cloud copy to restore from. Deleting a space you are currently playing is guarded by that same confirmation; treat the dialog as the last line of defense.

## Step 6 — Reset a run while Lab is connected

- **Action**: Start a session on "Main space", connect Lab (as in Tutorial 01), then reset the run inside Crystal Sprint.
- **You should see**: In Lab, the next operation is rejected with a "session changed" message.
- **If not**: If an operation succeeds, you were issued it after the reset — operations created against the new generation work normally.

### Why: the generation counter

Every session start, session stop, and game reset bumps a **generation counter**. Lab stamps operations with the generation they were issued under, and Space rejects stale ones with an actionable "session changed" message rather than letting an operation land against the wrong run. After a reset, simply re-run the operation — the search state from before the reset does not silently apply to the reset run.

## Housekeeping summary

| Action | Affects save data? |
| --- | --- |
| Rename a space | No — display name only |
| Delete a space | Yes — that space's saves are permanently removed, after confirmation |
| Stop a session | No — field values are saved; falling crystals are not |
| Reset a run | Resets the run and bumps the session generation |
| Uninstall Space | Yes — app-private storage is removed with the app |

## Where to go next

- [USER_GUIDE.md](USER_GUIDE.md) — the complete screen reference.
- [PRIVACY.md](PRIVACY.md) — exactly what data exists and how to remove all of it.
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — stale-generation errors and storage warnings.
