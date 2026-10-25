# Compatibility Matrix

What works, what is planned, and what will never be added — per capability, honestly. Statuses: **shipped** (in this build, at least unit-tested where applicable), **experimental** (present but rough), **planned** (intended, not started), **not-planned** (will not happen). A machine-readable mirror of this table lives in [compatibility.json](compatibility.json).

No capability currently holds the "experimental" status. The closest candidate — the third-party guest runtime — is marked *planned* below, because it does not exist in any form in this build and its feasibility is not yet proven.

## Suite identity

| Property | Value |
| --- | --- |
| Suite version | versionName 0.1.0, versionCode 1 (both apps) |
| Lab package | `io.github.siddhantpardeshi.aetherplay.lab` |
| Space package | `io.github.siddhantpardeshi.aetherplay.space` |
| Bridge protocol | 1.0 (major.minor), AIDL `IBridgeBroker`, capability masks |
| Bridge guard | Signature-level permission `io.github.siddhantpardeshi.aetherplay.BRIDGE` |
| Signing requirement | Any installed pair must share one signing key |

## Capability matrix

| Capability | Status | Notes |
| --- | --- | --- |
| Paired build and install (Lab + Space) | shipped | Both apps built from one tree; debug pair works out of the box (shared debug keystore). |
| Lab-to-Space bridge (AIDL `IBridgeBroker`, protocol 1.0) | shipped | Signature-level permission `io.github.siddhantpardeshi.aetherplay.BRIDGE`. Protocol negotiation is unit-tested. **Not device-tested** over real Binder. |
| Search: exact / range / unknown | shipped | Unit-tested (exact/range/unknown across regions, cancellation, result cap, bad queries). |
| Search: 10 value types (I8..F64), little-endian | shipped | Value codec unit-tested (parse/format/overflow/endianness/signedness/NaN/tolerance). |
| Search: alignment 1/2/4, region selection | shipped | Unit-tested. |
| Refine ops (exact, changed, unchanged, increased, decreased) | shipped | Unit-tested. |
| Read; write with preview then commit; per-item outcomes | shipped | Commit re-validates. Writable field set is bounded; `playfield` is read-only. |
| Undo (last write, identity re-check) | shipped | Last write only; refuses when the value is no longer yours or the session changed. |
| Freeze (300 ms re-apply, stop-all) | shipped | Re-apply loop, not a hard lock. All freezes stop with the session. |
| Result paging (100/page, cap 100,000; "changed" markers) | shipped | |
| Saved experiments (local JSON, export/share, one-tap rerun) | shipped | Local-only by design. |
| Named spaces with separate per-space saves | shipped | Files in app-private storage; storage usage shown. |
| Crystal Sprint training game (original, offline) | shipped | Runs inside Space; cooperative target only. |
| Themes: dark / OLED / light; reduce-motion switch | shipped | Light theme is less polished than dark. |
| Third-party guest runtime (other installed games) | planned | The "experimental milestone." Not started; feasibility documented but **not proven**; spike pending. Central unresolved item ([STATUS.md](STATUS.md) M3). |
| Scripts / Lua automation | not-planned | Not present in 0.1.0; no committed timeline. |
| Pattern / text and pointer searches | not-planned | Not present in 0.1.0; no committed timeline. |
| Floating overlay controls | not-planned | Not present in 0.1.0; no committed timeline. |
| Root mode | not-planned | Not present in 0.1.0; no committed timeline. |
| Anti-cheat evasion | not-planned | Out of scope forever. |
| Online / competitive / server-authoritative game support | not-planned | AetherPlay does not and will not target these. |
| Room database | not-planned | Simple JSON persistence is the chosen design. |
| Cloud / accounts / telemetry | not-planned | Fully local, permanently. Neither app requests the INTERNET permission. |
| On-device validation (incl. iQOO Neo 6) | planned | Physical validation pending. Not device-tested. |

## Platform support

| Dimension | Statement |
| --- | --- |
| Android versions | minSdk 23 (Android 6.0) through target/compileSdk 37 (intent). **Not tested on any physical Android version — not device-tested.** |
| ABIs | Pure Kotlin/Compose — no native code. arm64-v8a, armeabi-v7a, x86, and x86_64 all run the same bytecode; no per-ABI artifacts ship. |
| 16 KB page size | Not applicable in the strict sense: with no native libraries there are no native allocations to align. Stated here for completeness. |
| Per-build pairing | Lab and Space must be signed with the **same key** in any installed pair. Debug+debug (one machine) or release+release (one keystore). Mixed pairs fail the signature check by design. See [RELEASE_AND_SIGNING.md](RELEASE_AND_SIGNING.md). |

## How to read this matrix

- **shipped** means present in the 0.1.0 codebase and, where the capability is JVM-pure (codec, engine, protocol), covered by unit tests. It does **not** imply on-device validation — none exists.
- **planned** means intended future work with nothing in the tree. The two planned rows (guest runtime, device validation) are the project's open frontiers.
- **not-planned** rows fall into two groups: items absent from 0.1.0 with no committed timeline (scripts, pattern/pointer search, overlay, root), and permanent scope exclusions (anti-cheat evasion, online-game targeting, cloud/telemetry, Room). The distinction is spelled out in [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).
- For the honest, narrative version of this table, read [STATUS.md](STATUS.md).

## Scope statement

AetherPlay is an education and local-experimentation tool for games you own or have permission to test. It works today against its own training game only. It does not and will not target online, competitive, or server-authoritative games, and anti-cheat evasion is out of scope forever.
