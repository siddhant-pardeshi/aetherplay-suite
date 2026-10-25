# AetherPlay User Guide

This guide covers every screen in both apps: AetherPlay Lab (the instrument) and AetherPlay Space (the stage). It describes what each control does and, just as importantly, what AetherPlay does not do. If a capability is not listed here as working, assume it is not in this build — the canonical list is [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).

## The mental model

- **Space owns the session.** A session is a run of the Crystal Sprint training game's memory surface, hosted by a foreground service in Space. Sessions are always started from Space's UI. Lab only connects to an already-running session; it cannot start one (by design, per Android's background-start rules for foreground services).
- **Lab is a lens, not a key.** Lab reads and writes a small, bounded set of fields that Space intentionally exposes. It cannot touch arbitrary process memory.
- **Everything is local.** No accounts, no cloud, no telemetry. See [PRIVACY.md](PRIVACY.md).

---

## AetherPlay Lab

### Home

The entry point. Lab shows its connection state and is where you attach to a running Space session. If no session is running, Lab tells you to start one in Space first — that is intended behavior, not an error.

### Search composer

Where every search begins.

- **Type** — 10 supported value types, all little-endian:

  | Group | Types |
  | --- | --- |
  | Integers, signed | I8, I16, I32, I64 |
  | Integers, unsigned | U8, U16, U32, U64 |
  | Floating point | F32, F64 |

- **Mode** — how to match:
  - *exact* — value equals a number you provide.
  - *range* — value falls between two bounds you provide.
  - *unknown* — capture candidates by position only; you do not know the value yet. This is the starting point for [TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md](TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md).
- **Advanced** — alignment (1, 2, or 4 bytes) and region selection. Alignment narrows candidate addresses; region selection limits the scan to the regions you choose. Two regions exist in the training game: `state` (writable) and `playfield` (read-only telemetry).
- **Run** — executes the search. Results are bounded at 100,000 candidates; beyond that, the scan stops at the cap. A long scan can be cancelled.

### Results

Candidates, paged 100 per page with a *Load more* control. Each result shows its address and value, and is marked **changed** when the value has moved since it was captured. If you see far more results than you expected, that is usually the training game's deliberate mirrors — see the troubleshooting entry "No search results / too many results" in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

### Refinement

From a result set, refine with one of five operations:

| Operation | Keeps addresses whose value... |
| --- | --- |
| Exact value | equals a number you provide |
| Changed | differs from the last capture |
| Unchanged | is the same as the last capture |
| Increased | is higher than at the last capture |
| Decreased | is lower than at the last capture |

Refining repeatedly against a game that keeps changing its values is the core skill — mirrors and decoys drift differently from the real field, so they fall away.

### Result detail sheet: preview, then commit

Tapping a result opens a detail sheet for reading and writing. Writing is deliberately two-phase:

1. **Preview** — you enter the new value; Lab shows the old and new value for the address before anything happens.
2. **Commit** — Lab re-validates the write at commit time and reports a per-item outcome. Nothing is written silently.

Reads and writes outside the writable field set are rejected as read-only. The `playfield` region is read-only by design.

### Freeze

A freeze re-applies its value every 300 ms while the session runs. It is a re-apply loop, not a hard lock — a determined game can briefly overwrite the value between ticks. All freezes, without exception, stop when the session stops. Active freezes are listed in the History tab, which also has a *Stop all* control.

### History tab

- **Change journal** — the writes made this session, in order.
- **Undo** — restores the **last write only**, and only if the current value still equals what was written (an identity re-check). If the game has since changed the value, or the session generation changed, undo refuses and tells you why.
- **Freezes** — the active freeze list and *Stop all*.

### More tab

- **Tutorial** — the built-in walkthrough (the same path as [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md)).
- **Experiments** — saved search recipes: name, type, mode, value(s), alignment, and regions. Stored locally as JSON in Lab; export or share as JSON; run again in one tap. Recipes store search parameters, not results, and never leave the device unless you share them.
- **Settings** — theme and motion (below).
- **About** — versions and scope statement.

---

## AetherPlay Space

### Library

The game library. It contains exactly one game in this build: **Crystal Sprint** — an original, offline training game that runs inside Space. Third-party games cannot be added in this build; a guest runtime is future, unproven work ([STATUS.md](STATUS.md), milestone M3).

### Spaces

Named spaces let you keep separate Crystal Sprint save data — one save per space, stored as files in app-private storage, with per-space storage usage shown.

- **Create** a space, **rename** it, or **delete** it. Deleting asks for confirmation, because deletion removes that space's save data permanently.
- Which space's save you play depends on the space the session is started on. See [TUTORIAL_03_SPACES_AND_SAVE_DATA.md](TUTORIAL_03_SPACES_AND_SAVE_DATA.md).

### Session

Starting a session launches a foreground service (type `specialUse`) with an ongoing notification carrying *Open game* and *Stop session* actions. On Android 13+, Space requests `POST_NOTIFICATIONS`; if you deny it, the session still runs — you just lose the visible notification. Stopping the session from the notification or from Space ends the memory surface: every freeze stops, and Lab is disconnected from the session.

### Settings

Theme and motion preferences, mirroring Lab's (below).

---

## Sessions and the generation counter

Every session start, session stop, and game reset bumps a **generation counter**. Lab stamps its in-flight operations with the generation they were issued under. If the generation changes — you reset the run, or the session ended and restarted — stale operations are rejected with an actionable "session changed" message instead of silently applying to the wrong run. If you see that message, reconnect or re-run the operation against the current generation.

## Themes and motion

Both apps use the **Aurora Precision** design system: dark `#0B0D12` is the default, with OLED and light themes available; Lab's accent is electric mint `#65E3BE`, Space's is soft violet `#B5A1FF`. An animated aurora backdrop plays in both apps, and a **reduce-motion** switch in each app's settings turns it off. The light theme is functional but less polished than dark — noted in [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).

## Honest capabilities

| Works today | Not in this build |
| --- | --- |
| Search: exact / range / unknown, 10 types, alignment, regions | Third-party guest runtime (no other installed game can be touched) |
| Refine: exact, changed, unchanged, increased, decreased | Scripts / Lua automation |
| Read; write with preview then commit; last-write undo | Pattern/text and pointer searches |
| Freeze (300 ms re-apply), stop-all | Floating overlay controls |
| Saved experiments (local JSON, shareable) | Root mode; anti-cheat evasion (out of scope forever) |
| Named spaces with separate saves | Cloud, accounts, telemetry (fully local) |

AetherPlay does not and will not target online, competitive, or server-authoritative games. It is an education and local-experimentation tool for games you own or have permission to test.
