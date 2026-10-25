# Troubleshooting

Most problems fall into a small number of causes: an unpaired install, no running session, or an Android 13+ notification denial. Work top to bottom. If nothing here helps, see "Bug reports" at the end.

## Quick symptom table

| Symptom | Most likely cause | Jump |
| --- | --- | --- |
| "Space is not connected" in Lab | Space not installed / session not running / signature mismatch | [A](#a-space-is-not-connected) |
| Version or protocol mismatch message | Apps from different builds | [B](#b-version--protocol-mismatch) |
| "No active session" | Session stopped, or never started | [C](#c-no-active-session) |
| "Session ended" notification in Lab | Space stopped the session | [D](#d-session-ended-notifications) |
| "Session changed" / stale-generation error | Session restarted or run reset mid-operation | [E](#e-stale-generation-errors-after-a-reset) |
| No search results | Wrong current value, or mirrors moved | [F](#f-no-search-results) |
| Freeze not holding | Read-only address or stopped session | [G](#g-freeze-not-holding) |
| No session notification | Notification permission denied | [H](#h-notification-permission-denied) |
| Storage deletion warning in Space | You are deleting a space's save data | [I](#i-storage-deletion-warnings) |
| `adb install` fails | Stale signature or old install | [J](#j-adb-install-tips) |

## A. "Space is not connected"

Lab cannot see Space's bridge service. Causes, in order of likelihood:

1. **Space is not installed.** Lab binds to package `io.github.siddhantpardeshi.aetherplay.space` by ID; nothing else can stand in. Install Space ([QUICKSTART.md](QUICKSTART.md) step 4).
2. **No session is running.** Lab attaches to an already-running session; it cannot start one (by design). Start a session in Space's UI first, then connect from Lab.
3. **Signature mismatch.** The bridge is guarded by the signature-level permission `io.github.siddhantpardeshi.aetherplay.BRIDGE`, so both apps must be signed with the **same key**. A debug Lab against a release Space (or vice versa) fails the check. Fix: install a matched pair — both debug (default when building from one machine) or both signed with the same release keystore. See [RELEASE_AND_SIGNING.md](RELEASE_AND_SIGNING.md).

## B. Version / protocol mismatch

The bridge speaks a major.minor protocol (currently 1.0). If Lab and Space were built from different versions, Lab reports a version or protocol mismatch instead of attempting an incompatible conversation. Fix: rebuild and reinstall **both** APKs from the same source tree — one `./gradlew :apps:lab:assembleDebug :apps:space:assembleDebug` produces a matched pair.

## C. "No active session"

The session ended (see D) or was never started. Sessions exist only while Space's foreground service runs them. Start a session from Space's UI. Lab will never start one for you — this is a deliberate consequence of Android's background-start rules for foreground services, not a missing feature.

## D. "Session ended" notifications

When a session stops — you pressed *Stop session*, Space's process was killed by the OS, or the phone rebooted — Lab tells you the session ended. Everything tied to the session is already gone: all freezes stopped and the change journal is closed. Nothing needs cleanup; start a new session in Space and reconnect.

If sessions end on their own repeatedly, your device's battery optimization is likely killing Space. Exempt Space from battery optimization during experiments (see the OEM-specific notes in [IQOO_NEO6_SETUP.md](IQOO_NEO6_SETUP.md); the same advice generalizes to other aggressive OEMs).

## E. Stale-generation errors after a reset

Every session start, session stop, and game reset bumps a session **generation counter**. Operations Lab issued under an older generation are rejected with an actionable "session changed" message. This guard exists so an in-flight search or write can never land against a run it was not created for. Fix: after a reset or restart, simply re-issue the operation against the current session. Nothing is corrupted; the refusal is the system working.

## F. No search results

1. **The value changed while you searched.** Energy drains, tokens drift, mirrors lag. Re-check the live value and search again, or switch to **unknown** mode and narrow by refinement instead of guessing the exact value — the full workflow is [TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md](TUTORIAL_02_SEARCH_REFINE_AND_UNDO.md).
2. **Expect too *many* results, actually.** The training game deliberately exposes mirrors: the real crystals field at `0x7B000000`, a mirror at `0x7B100078` in the read-only `playfield` region, and lagging display copies. A correct search usually returns several addresses. Refine with *Increased* / *Decreased* / *Changed* / *Unchanged* after changing the value in-game, and the copies fall away. Walkthrough: [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md).
3. **Type or alignment is wrong.** Crystals is Int32; energy and speed are Float32; best is Int64. Alignment 4 matches the training game's fields.

## G. Freeze not holding

- **The address is read-only.** Everything in the `playfield` region (base `0x7B100000`) is read-only telemetry; freezes cannot hold there. Freeze the writable counterpart in the `state` region (base `0x7B000000`).
- **The session stopped.** All freezes stop when the session stops, always. Check the History tab — the freeze list should be empty after a session ends. Start a new session and re-freeze.
- **You are seeing the 300 ms tick.** A freeze re-applies its value every 300 ms; it is a re-apply loop, not a hard lock, so the value can visibly flicker between ticks. That is the documented behavior, not a failure.

## H. Notification permission denied

On Android 13+, Space requests `POST_NOTIFICATIONS`. If denied, the session **still runs** — you lose the visible notification and its *Open game* / *Stop session* actions. Space handles the denial gracefully. You can start the game from Space's Library and stop the session from Space's UI. To get the notification back, re-enable notifications for AetherPlay Space in system settings and start a new session.

## I. Storage deletion warnings

When you delete a named space, Space warns you and asks for confirmation, because deletion permanently removes that space's save files (crystals, energy, best, gems, tokens, run time) from app-private storage. There is no undo and no cloud copy. Cancel is always safe. Details: [TUTORIAL_03_SPACES_AND_SAVE_DATA.md](TUTORIAL_03_SPACES_AND_SAVE_DATA.md).

## J. adb install tips

- Use `adb install -r` to replace an existing install while keeping data.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` means the installed app is signed with a different key than the APK you are installing (typical when switching between debug and release builds, or between machines). Uninstall both apps first — `adb uninstall io.github.siddhantpardeshi.aetherplay.space && adb uninstall io.github.siddhantpardeshi.aetherplay.lab` — then install a matched pair.
- Always update **both** apps together; the bridge requires matching signatures and a compatible protocol version.

## Bug reports

Diagnostics are manual — there is no telemetry of any kind ([PRIVACY.md](PRIVACY.md)). To report an issue, reproduce it while capturing logs:

```bash
adb logcat
```

Include: both app versions (0.1.0 in this build), Android version, device, whether the builds are a matched pair, and the log excerpt around the failure. File it at the repository: `siddhant-pardeshi/aetherplay-suite`.
