# Project Status

Honest accounting against the milestone plan (M0–M6). "Done" means done and unit-tested where testing applies — it never means "tested on a device," because no device validation has happened at all in this project so far.

## Milestone table

| Milestone | Scope (from the project brief) | Status | Honest accounting |
| --- | --- | --- | --- |
| **M0** | Feasibility and research | **Partially done** | Feasibility documented. Critically, the third-party runtime is **not proven** — the feasibility spike is still pending. Documentation of the problem space exists; the hard technical question remains open. |
| **M1** | Foundation: paired build, authenticated versioned connection, design system, onboarding | **Done** | Two independently installable apps (Lab + Space) building from one tree; bridge with signature-level permission and protocol 1.0 major.minor negotiation plus capability masks (unit-tested); Aurora Precision design system (dark/OLED/light, reduce motion); onboarding tutorial content in place. Not exercised on a device. |
| **M2** | Lab vertical slice vs the training game: search, refine, read, previewed edit, undo, bounded freeze, cancel, stop | **Done** | All listed capabilities shipped against Crystal Sprint's memory surface: search (exact/range/unknown, 10 types, alignment, regions), refinement (exact/changed/unchanged/increased/decreased), read, preview-then-commit writes with per-item outcomes, last-write undo with identity re-check, 300 ms freeze with stop-all, cancellation, session stop. Engine and codec are unit-tested on the JVM. |
| **M3** | Guest runtime for third-party games | **Not done** | Nothing exists. This is the **central unresolved item** of the project: no virtual space, no guest runtime, no ability to touch any installed game other than Crystal Sprint. Feasibility documented but not proven (spike pending, carried over from M0). |
| **M4** | Polish and persistence | **Partially done** | Polish exists (Aurora Precision themes, reduce motion, aurora backdrop; light theme admittedly less refined) and persistence exists (named spaces with per-space saves in app-private storage, saved experiments as local JSON, storage usage display). Outstanding: OEM edge cases (battery optimization, notification management) and measured limits — no measurements exist. |
| **M5** | Device validation | **Not done** | No device validation of any kind has occurred. This includes the iQOO Neo 6, which has never been touched (physical validation pending — see [IQOO_NEO6_SETUP.md](IQOO_NEO6_SETUP.md)). Every on-device expectation in the docs describes intended behavior, not observed behavior. |
| **M6** | Release readiness | **Partially done** | Done: this documentation suite, and the demo-web hosted tour (labeled "Interactive product tour — simulated preview" plus a playable web adaptation of Crystal Sprint, simulations labeled at the point of interaction). Pending: clean-clone build pending CI verification; GitHub push in progress; real demo recording pending a device. |

## What would move each incomplete milestone

Without committing to dates, these are the concrete open items implied by the accounting above:

| Milestone | Open items |
| --- | --- |
| M0 | Run the third-party runtime feasibility spike; either a proof or a documented "not viable as scoped" decision. |
| M3 | Everything — gated on M0's outcome before design work is honest. |
| M4 | On-device OEM behavior (battery optimization, notification management) and measured limits: fill the results table in [PERFORMANCE.md](PERFORMANCE.md). |
| M5 | Run the paired builds on any real device; then the iQOO Neo 6 specifically; record observed behavior against the tutorials' "You should see" steps. |
| M6 | Verify a clean-clone build via CI; finish the GitHub push; record the real demo once a device is available. |

## Notes on the two "not done" milestones

**M3 (guest runtime)** is the milestone that would let AetherPlay host a third-party game inside a controlled runtime. It is gated on the M0 feasibility spike, deliberately: building a runtime before proving the approach is feasible would be documenting aspiration, not engineering. Until the spike runs, every statement about third-party support must stay in the future tense — which is why every page in this documentation suite repeats that Crystal Sprint is the only target.

**M5 (device validation)** blocks several other honest completions. The real demo recording (M6) needs a device; the performance results table ([PERFORMANCE.md](PERFORMANCE.md)) needs a device; the iQOO Neo 6 page ([IQOO_NEO6_SETUP.md](IQOO_NEO6_SETUP.md)) stays marked PHYSICAL VALIDATION PENDING until one is touched. This is the single highest-leverage open item in the project: a few hours with any Android device would convert a large amount of "intended behavior" into "observed behavior."

## What is tested today

| Area | Where | Method | State |
| --- | --- | --- | --- |
| Value codec (parse/format/overflow/endianness/signedness/NaN/tolerance) | `core:model` | JVM unit tests | Passing |
| Search engine (exact/range/unknown, alignment, refine ops, cancellation, 100k cap, bad queries) | `engine:search` | JVM unit tests | Passing |
| Protocol negotiation (major/minor, capability masks) | `core:protocol` | JVM unit tests | Passing |

Run: `./gradlew :core:model:test :engine:search:test`.

## What is not tested

- **The Binder path on a real device.** Lab connecting to Space over a real Android Binder transaction has never been executed — no device or emulator existed in the authoring session.
- **On-device behavior generally**: UI, session lifecycle, foreground-service behavior, notification permission flow, freeze timing as experienced, storage behavior.
- **The iQOO Neo 6** and every other physical device.
- **Performance.** No measurements exist; all budgets are targets ([PERFORMANCE.md](PERFORMANCE.md)).

## Where things live

| Artifact | Location |
| --- | --- |
| Repository (public) | `siddhant-pardeshi/aetherplay-suite` on GitHub |
| Hosted tour | `demo-web/` — self-contained, for GitHub Pages; includes the clearly labeled "Interactive product tour — simulated preview" and a playable web adaptation of Crystal Sprint |
| This documentation | `docs/` — start at [QUICKSTART.md](QUICKSTART.md) |
| Licenses | [../LICENSE](../LICENSE) (MIT) and [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) |

## Bottom line

Works today: the full Lab vertical slice against the training game, over an authenticated, version-negotiated bridge, unit-tested at the engine and protocol layers. Not yet: any third-party game support (the project's central open question) and every form of on-device validation. The next most valuable work, in order: run the pair on any real device; the M3 feasibility spike; clean-clone CI verification.

Related pages: [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md), [COMPATIBILITY.md](COMPATIBILITY.md), [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## v0.1.1 fixes (2026-10-26)

- Crystal Sprint now pauses whenever its activity is not active (verified live: a watched value stayed frozen across 6+ seconds of background time; previously the game silently kept playing).
- `stopSession` saves and detaches synchronously, so a fast process death can no longer lose game data.
- Lab no longer flashes "No active session" during the first status poll (Home and Search show a neutral state instead).
- Result rows for non-writable addresses show their real live value instead of "?" (readAt now serves any in-region address; writes remain restricted to the named fields).

## On-emulator validation (2026-10-26, before first publish)

Verified end-to-end on an Android 16 (API 36, x86_64, WHPX-accelerated) emulator with paired debug builds:

- Paired install; signature-guarded bridge connected (protocol 1.0 handshake) from a separately installed Lab.
- Session started from Space UI (foreground-service notification + POST_NOTIFICATIONS dialog handled).
- Crystal Sprint played live; exact Int32 search (121 and 99 match runs), unknown-value search (186).
- Refine Increased narrowed 121 -> 3 candidates against the live game.
- Preview write (old -> new shown before commit), commit journaled, and the game HUD reflected the written value lineage (999 -> kept growing from it).
- Undo ran with identity checking; freeze held the crystals value at 0 through sustained gameplay swipes.
- Stopping the session from Space released the session state cleanly.

Not covered by this run: physical devices (iQOO Neo 6 remains pending), performance measurements, and long soak stability.
The raw screen recordings for this session are the basis of the v0.1.0 walkthrough video (see Releases).
