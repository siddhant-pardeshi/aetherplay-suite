# Known Limitations

The complete, honest list of what AetherPlay 0.1.0 does not do. Nothing here is hidden in a footnote elsewhere: if a feature is not listed as shipped in [COMPATIBILITY.md](COMPATIBILITY.md), it is not in this build. Items marked *not tested* have never been exercised on hardware — no device or emulator was available in the authoring session.

## Scope-defining limitations

These draw the outer boundary of the project. They are consequences of design, not accidents.

1. **Training game only.** AetherPlay edits exactly one game: Crystal Sprint, the original offline training game inside Space. There is **no third-party guest runtime and no virtual space for other installed games** in this build. That capability — the "experimental milestone" — is not started and its feasibility is documented but **not proven** (spike pending). It is the central unresolved item of the project ([STATUS.md](STATUS.md), M3).
2. **Single writable game.** Consequently there is exactly one writable memory surface (the 64-byte `state` region). You cannot point Lab at anything else.
3. **No online, competitive, or server-authoritative games — ever.** AetherPlay is an education and local-experimentation tool for games you own or have permission to test. This is permanent scope, not a TODO.
4. **No anti-cheat evasion of any kind — ever.** Out of scope forever.
5. **No root mode.** Not present, not planned as a hidden path either; the Android sandbox applies to everything AetherPlay does.

## Feature limitations (present build)

Each of these is shipped behavior that stops short of what a seasoned user might expect from a general-purpose tool. That gap is deliberate and documented.

| # | Limitation | Detail and practical impact |
| --- | --- | --- |
| 6 | Undo is last-write-only | The change journal records the session's writes, but undo restores the **most recent committed write only**. No multi-step rewind. Undo also performs an identity re-check and refuses if the current value no longer equals what was written, or if the session generation changed. |
| 7 | Freeze is a 300 ms re-apply loop, not a hard lock | A frozen value is re-applied every 300 ms while the session runs; between ticks the game can briefly overwrite it, and you may see flicker. All freezes stop when the session stops, always. |
| 8 | Search is bounded to the cooperative surface | Lab scans only the regions Space exposes (`state`, `playfield`) — a few hundred bytes of deliberately designed memory — not arbitrary process memory. |
| 9 | Results cap at 100,000 candidates | Paged 100 per page with "Load more"; large scans truncate at the cap rather than run unbounded. |
| 10 | No scripts or Lua automation | Every action is manual. |
| 11 | No pattern/text search; no pointer search | Only exact, range, and unknown modes with refinement. |
| 12 | No floating overlay controls | AetherPlay is used app-to-app; expect window switching between Lab and the game. |
| 13 | Experiments are local-only JSON | Saved recipes live in Lab's private storage; sharing is a manual export of a JSON file. No sync, no import gallery, no community library. |
| 14 | Lab cannot start sessions — by design | Sessions start from Space's UI (Android foreground-service background-start rules). Lab only connects to an already-running session. |
| 15 | Light theme is less polished than dark | Dark `#0B0D12` is the design's home; OLED is close behind; light works but is the least refined of the three. |
| 16 | Simple JSON persistence, no database | A deliberate choice — no Room. It means no queries, no migrations, and app-private files as the only storage. |

## Validation limitations (not tested)

These are unknowns, not defects: things that may well work perfectly but have never been observed.

17. **No on-device testing of any kind has happened yet.** All "should see" expectations in the tutorials describe intended behavior, not observed behavior.
18. **The Lab-to-Space bridge over real Binder is not tested.** Protocol negotiation is unit-tested on the JVM; the over-the-wire path on a real device is not.
19. **The iQOO Neo 6 has never been touched.** The setup page for it is explicitly marked PHYSICAL VALIDATION PENDING: [IQOO_NEO6_SETUP.md](IQOO_NEO6_SETUP.md).
20. **No performance measurements exist.** Every budget in [PERFORMANCE.md](PERFORMANCE.md) is a target, not a result.
21. **Not tested on any physical Android version.** minSdk 23 / target 37 are build settings, not validated coverage.

## What IS tested

For balance, and so the boundary is clear — these have JVM unit tests:

| Area | Module | Coverage |
| --- | --- | --- |
| Value codec | `core:model` | Parse/format, overflow, endianness, signedness, NaN, tolerance — across all 10 types. |
| Search engine | `engine:search` | Exact/range/unknown across regions, alignment, all refine ops (including increased/unchanged/exact), cancellation, the 100,000-result cap, bad-query handling. |
| Protocol negotiation | `core:protocol` | Major/minor version negotiation and capability masks. |

Run them: `./gradlew :core:model:test :engine:search:test`.

## Honesty policy

This project documents reality, not aspiration. If a claim in these docs overstates what the code does, that is a bug in the docs — the same way a crash is a bug in the code. When in doubt: works today = listed as shipped and, where applicable, unit-tested; not yet = listed here.

## Where each limitation is discussed elsewhere

| Limitation group | Covered in depth by |
| --- | --- |
| Training-game-only scope, guest runtime | [STATUS.md](STATUS.md) (M0, M3), [ARCHITECTURE.md](ARCHITECTURE.md) |
| Undo and freeze semantics | [TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md](TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md), [USER_GUIDE.md](USER_GUIDE.md) |
| Search bounds, mirrors, "no results" | [TROUBLESHOOTING.md](TROUBLESHOOTING.md) |
| Spaces, deletion, what persists | [TUTORIAL_03_SPACES_AND_SAVE_DATA.md](TUTORIAL_03_SPACES_AND_SAVE_DATA.md) |
| Device validation gaps, iQOO Neo 6 | [IQOO_NEO6_SETUP.md](IQOO_NEO6_SETUP.md), [COMPATIBILITY.md](COMPATIBILITY.md) |
| Performance (no measurements) | [PERFORMANCE.md](PERFORMANCE.md) |
| Release signing pairing | [RELEASE_AND_SIGNING.md](RELEASE_AND_SIGNING.md) |
