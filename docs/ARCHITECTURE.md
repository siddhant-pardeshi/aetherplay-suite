# Architecture

How the AetherPlay Suite is put together: modules, the Binder bridge, the session lifecycle, the search/write/freeze pipelines, the security model, and why the build is pinned the way it is.

## Module map

| Module | Kind | Responsibility |
| --- | --- | --- |
| `core/model` | JVM | Value types, codec, and shared primitives. Pure JVM: unit-testable without a device. |
| `core/protocol` | Android library | The AIDL bridge contract: `IBridgeBroker`, protocol version (1.0, major.minor), capability negotiation. |
| `core/design` | Android library | The "Aurora Precision" design system: themes (dark `#0B0D12` default, OLED, light), accents (Lab electric mint `#65E3BE`, Space soft violet `#B5A1FF`), aurora backdrop with reduce-motion support. |
| `engine/search` | JVM | The search engine: scan, refine, paging, cancellation, candidate cap. Pure JVM, heavily unit-tested. |
| `apps/lab` | Android app | The instrument: search/refine/read/write/undo/freeze UI, saved experiments (JSON). |
| `apps/space` | Android app | The stage: named spaces, the session foreground service, Crystal Sprint, and the bridge service host. |
| `demo-web` | Static site | Self-contained hosted tour for GitHub Pages: a clearly labeled "Interactive product tour — simulated preview" of the Lab workflow plus a playable web adaptation of Crystal Sprint. Simulations are labeled at the point of interaction. |
| `scripts` | Tooling | e.g. `generate_icons.py` (original icon assets). |

## The Binder bridge

Lab and Space are separate processes in separate packages. Lab binds to a service exported by Space with action `io.github.siddhantpardeshi.aetherplay.BRIDGE` on package `io.github.siddhantpardeshi.aetherplay.space`, and speaks AIDL through `IBridgeBroker`:

```
+----------------------+    bindService(action=BRIDGE,                    +----------------------+
|    AetherPlay Lab    |    package=...aetherplay.space)                  |   AetherPlay Space   |
|    (instrument)      | ------------------------------------------------> |   bridge service     |
|                      |                                                   |                      |
|  search / refine /   | <------------ IBridgeBroker (AIDL) -------------> |  session host:       |
|  read / write /      |      guarded by signature-level permission       |  - memory surface    |
|  undo / freeze       |      io.github.siddhantpardeshi.aetherplay.BRIDGE|  - generation guard  |
|                      |      protocol 1.0 (major.minor) + capabilities   |  - FGS specialUse    |
+----------------------+                                                   +----------------------+
        |                                                                          |
        |  Lab NEVER starts sessions. Sessions start from Space's UI only.          |
        +----------------------------------------------------------------------->--+
```

Key properties:

- **Protocol negotiation.** Version 1.0 as major.minor, with capability masks; negotiation is unit-tested in `core/protocol`. A version or protocol mismatch is reported, not guessed at.
- **Authentication.** The service is guarded by the signature-level permission `io.github.siddhantpardeshi.aetherplay.BRIDGE`; enforcement is per-call (`enforceCaller`), not only at bind time.
- **Direction.** Lab connects to an already-running session. It cannot start one — this follows Android's background-start rules for foreground services (below).

## Session lifecycle

```
                 start (Space UI only)              stop (UI or notification)
    NONE --------------------------------> RUNNING ------------------------> STOPPING ----> NONE
     ^                                                                                  |
     |                            generation++ at start, stop, and game reset            |
     +------------------------------------------------------------------------------------+
```

- `NONE -> RUNNING`: the user starts a session from Space's UI; the foreground service (type `specialUse`) starts, the ongoing notification appears with *Open game* / *Stop session* actions, the memory surface comes up, the generation counter bumps.
- `RUNNING -> STOPPING -> NONE`: the session tears down. All freezes stop, the change journal closes, Lab is disconnected.
- **Generation counter.** Bumps on session start, session stop, and game reset. Every operation Lab issues carries the generation it was created under; Space rejects stale operations with an actionable "session changed" message rather than letting them land on the wrong run.

## Search pipeline

```
query (type, mode, value(s), alignment, regions)
   -> spec (validated)
   -> engine scan over the selected regions
   -> candidates (bounded at 100,000; cancellable mid-scan)
   -> pages (100 per page, "Load more"; values marked "changed" when moved since capture)
```

Refinement re-captures and filters: exact / changed / unchanged / increased / decreased. Because the training game keeps its values moving (drifting decoy, mirrors, lagging copies), refinement converges — that is the skill being taught, and the game is designed for it.

## Write pipeline

```
preview (old -> new shown per address; nothing written)
   -> pending
   -> commit (re-validated at commit time; per-item outcomes)
   -> change journal (in-session)
   -> undo (LAST write only; identity re-check: applies only if the
            current value still equals what was written)
```

Reads and writes outside the bounded writable field set are rejected as read-only. The `playfield` region (base `0x7B100000`, 128 bytes) is read-only telemetry by design — it exists so that refinement is meaningful.

## Freeze loop

```
while (session generation unchanged):
    re-apply every frozen value   # every 300 ms
on session stop: ALL freezes stop, always
```

A freeze is a 300 ms re-apply loop, not a hard lock — a game can briefly overwrite between ticks. Space-side (or Lab History's *Stop all*) termination is the only exit condition that matters; session stop wins unconditionally.

## The memory surface (training game)

| Region | Base | Size | Access | Contents |
| --- | --- | --- | --- | --- |
| `state` | `0x7B000000` | 64 bytes | bounded writable | crystals I32 @+0; energy F32 @+4 (clamped 0..100); best I64 @+8; speed F32 @+16 (clamped 0.25..4); gems I32 @+20; tokens I32 @+24 (drifting decoy); runTime F32 @+28 |
| `playfield` | `0x7B100000` | 128 bytes | read-only | telemetry: mirrors and lagging copies (e.g. a crystal mirror at `0x7B100078`) |

## Security model

- **Signature-level bridge permission** (`io.github.siddhantpardeshi.aetherplay.BRIDGE`): only apps signed with the same key as Space may talk to the broker. Both apps in a pair must share a signing key.
- **Per-call enforcement** (`enforceCaller`): authorization is checked on every call, not merely at bind time.
- **Generation guards**: stale operations are rejected, never best-effort applied.
- **Bounded writable field set**: writes are only honored inside the defined writable fields of `state`; everything else (and all of `playfield`) is read-only.
- **No arbitrary process access**: Lab cannot scan or write other processes' memory. The Android sandbox applies to everything; AetherPlay works only against the surface Space itself exposes in its own process.
- **No network**: neither app requests the INTERNET permission ([PRIVACY.md](PRIVACY.md)).
- **Scope**: no anti-cheat evasion of any kind, ever; no online/competitive/server-authoritative targets.

## Why sessions start from Space's UI

Android restricts starting foreground services from the background. Space's session is a user-visible, user-initiated foreground service (type `specialUse`) with an ongoing notification — the honest way to host a long-running memory surface. Lab is a client that binds after the fact; it never needs (and never gets) the ability to spin up a session on its own. This also keeps the trust direction right: the app that owns the data hosts the session; the instrument only observes and edits what the host exposes.

## Persistence model

Deliberately simple: **JSON files, no database**. Space stores space metadata and per-space Crystal Sprint saves as files in app-private storage; Lab stores saved experiments (search recipes) as JSON locally. There is no Room, no ContentProvider, no sync. Deletion is honest: delete a space (with confirmation) or uninstall the app, and the data is gone.

## Build and toolchain

| Pin | Value | Why |
| --- | --- | --- |
| Gradle wrapper | 9.8.2 | Locked via the wrapper; reproducible builds. |
| AGP | 9.4.1 | Current Android Gradle Plugin. |
| Kotlin | 2.4.21 | One external Kotlin toolchain pinned across app/library/JVM modules for deterministic compiler and Compose plugin parity. |
| Compose BOM | 2026.09.00 | Material 3; aligned with the pinned Kotlin. |
| SDK | compileSdk 37, targetSdk 37, minSdk 23 | Modern target, broad reach. |
| JDK | 17 | Required for the toolchain. |

**AGP 9 built-in-Kotlin opt-out.** AGP 9 defaults to its built-in Kotlin. This project pins one external Kotlin toolchain instead, via the documented opt-out in `gradle.properties`:

```properties
android.builtInKotlin=false
android.newDsl=false
```

This keeps app, library, and JVM modules on the same compiler and the Compose plugin on a matching version. Note this opt-out path is documented as removed in AGP 10 — an AGP 10 upgrade will need to migrate onto built-in Kotlin. That migration is not done yet (nothing has been upgraded; 9.4.1 is current).

## Testing posture (honest)

- **Unit-tested (JVM)**: value codec (parse/format/overflow/endianness/signedness/NaN/tolerance) in `core:model`; search engine (exact/range/unknown across regions, alignment, refine ops, cancellation, result cap, bad queries) in `engine:search`; protocol negotiation (major/minor, capability masks) in `core:protocol`. Run: `./gradlew :core:model:test :engine:search:test`.
- **Not tested**: on-device behavior generally, and specifically the Lab-to-Space path over real Binder — no device or emulator was available in the authoring session. See [STATUS.md](STATUS.md) and [PERFORMANCE.md](PERFORMANCE.md).
