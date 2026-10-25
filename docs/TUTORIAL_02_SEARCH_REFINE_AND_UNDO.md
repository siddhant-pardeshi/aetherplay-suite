# Tutorial 02 — Unknown Search, Refinement, and Undo

Tutorial 01 started from a value you could read off the HUD. This tutorial starts from the harder, more realistic case: a value you cannot read exactly. You will use the **unknown** search mode on the energy field, narrow it with refinements, write to it, and learn exactly when undo will and will not help you.

Prerequisites: a paired install and one completed run of [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md).

## The target: energy

Crystal Sprint's energy is a **Float32** at `0x7B000004` (offset +4 in the `state` region). The game clamps it to the range 0..100. Like the crystal count, it has copies elsewhere — but unlike crystals, you may not know its exact value at any moment, which makes it the perfect target for unknown search.

## Step 1 — Start a session and open the game

- **Action**: Start a session in Space, open Crystal Sprint, and play until energy has visibly moved at least once.
- **You should see**: The session notification is present and the game is running.
- **If not**: See Step 1 recovery in [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md).

## Step 2 — Unknown search

- **Action**: In Lab's search composer, set type **F32**, mode **unknown**, alignment **4**, regions both `state` and `playfield`. Run the search.
- **You should see**: A large candidate list. Unknown mode records addresses by position without knowing their values, so every aligned Float32-sized slot in the scanned regions is a candidate. The list is capped at 100,000 candidates.
- **If not**: If the search feels slow, cancel it — cancellation is supported — and narrow the region selection to `state` only, then run again.

## Step 3 — Play, then refine with Changed

- **Action**: Switch to the game and play until energy moves.
- **Action**: Back in Lab, refine with **Changed**.
- **You should see**: The list shrinks sharply. Every address whose value did not move is discarded.
- **If not**: Almost nothing left? You refined against a capture taken too long after the search; restart from Step 2 and keep the gaps between captures short.

## Step 4 — Refine with Decreased (or Increased)

- **Action**: Play until energy moves again, in a known direction — energy typically falls as you sprint, so **Decreased** is the usual refinement.
- **You should see**: The list shrinks again.
- **If not**: If you refined with the wrong direction and emptied the list, undo is not available for refinements (undo covers writes only) — restart from Step 2. Refinement filters are one-way.

## Why refinement works at all

The game keeps changing its values. While you play, energy drains, the run timer advances, tokens (a deliberately drifting decoy field) wander on their own, and the read-only `playfield` region holds mirrors and lagging copies that track the real fields imperfectly. Each refinement compares a fresh capture against the last one and keeps only addresses that behaved the way you asserted. Real fields and mirrors follow the game; lagging copies follow late; decoys follow nothing. Three or four well-chosen refinements usually collapse thousands of candidates to a handful.

## Step 5 — Finish with Exact (when you know the value)

- **Action**: Refine with **Exact value**, entering the energy value if you can read it from the game's HUD.
- **You should see**: A short list. The real field `0x7B000004` should be in it.
- **If not**: If you cannot read the exact value from the HUD, keep alternating **Changed**/**Unchanged** instead — the exact refinement is a convenience, not a requirement. (Values that moved since capture are marked *changed* in the results list, which helps you decide which refinement to apply next.)

## Step 6 — Write to energy

- **Action**: Tap the result at `0x7B000004`. Enter a target inside the clamp range — say **100** — and **Preview**, then **Commit**.
- **You should see**: The preview shows the old and new value for the address; commit reports a per-item outcome and the change appears in the History tab's journal.
- **If not**: If the sheet reports the address is read-only, you selected a `playfield` copy — go back and pick the `state`-region address. If the game clamps your written value back into 0..100, write inside the range.

## Step 7 — Undo, and what undo actually promises

- **Action**: From the History tab, undo.
- **You should see**: The last write is restored and the journal reflects it.
- **If not**: Undo may refuse. That refusal is correct behavior, not a malfunction.

### Undo semantics, precisely

Undo is deliberately narrow:

1. **Last write only.** Only the most recent committed write can be undone. There is no multi-step history rewind in this build.
2. **Identity re-check.** Undo applies only if the address's current value still equals what was written. If the game has changed the field since your write, the value is no longer "yours" — restoring it could clobber real game state, so undo refuses.
3. **Session-scoped.** The change journal lives in-session. Stopping the session, starting a new one, or resetting the run bumps the generation counter, and stale operations — undo included — are rejected with an actionable "session changed" message.

In short: undo is a "take back my last edit" button, not a time machine. If the game has already moved on, the honest answer is to write a new value rather than rewind an old one.

## Step 8 — Stop cleanly

- **Action**: Stop the session from Space or the notification.
- **You should see**: Freezes (if any) stop, Lab loses the session, and nothing remains running.
- **If not**: See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Practice ideas

- Repeat the workflow on **speed** (Float32 at `0x7B000010`, clamped 0.25..4) and notice the clamp pulling out-of-range writes back in.
- Hunt **tokens** (Int32 at `0x7B000024`) with unknown search — it drifts on its own, so *Changed*/*Unchanged* refinements expose it quickly. It is a decoy by design.
- Save a winning recipe (type, mode, values, alignment, regions) as an **experiment** in Lab's More tab, then re-run it in one tap next session.

## Where to go next

- [TUTORIAL_03_SPACES_AND_SAVE_DATA.md](TUTORIAL_03_SPACES_AND_SAVE_DATA.md) — separate saves per space, deletion, reset behavior.
- [USER_GUIDE.md](USER_GUIDE.md) — the complete screen reference.
