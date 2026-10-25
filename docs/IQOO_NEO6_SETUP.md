# AetherPlay on the iQOO Neo 6 — Expected Setup

> **PHYSICAL VALIDATION PENDING.** AetherPlay has **never been run on a physical iQOO Neo 6**. No iQOO Neo 6 was available in the authoring session; nothing below is a confirmed observation. This page describes the *expected* setup based on the suite's requirements and on how Funtouch OS generally behaves. Treat every step as "should work, not yet verified," and treat any failure as data worth reporting, not a surprise.

## Expected requirements

The iQOO Neo 6 should be a compatible host. From the suite's side, the requirements are:

- Android 6.0 (API 23) or newer (the suite targets minSdk 23, targetSdk 37).
- No rooting, unlocking, or flashing — required or suggested. AetherPlay has no root mode and never will.
- No native code runs in either app (pure Kotlin/Compose), so the device's ABI does not matter.
- Both AetherPlay apps installed **as a pair** from one build.

## Expected setup, step by step

1. **Install both APKs as a pair** — `space-debug.apk` and `lab-debug.apk` built in one session (paired debug builds share the debug keystore, so the signature check passes):

   ```bash
   adb install -r apps/space/build/outputs/apk/debug/space-debug.apk
   adb install -r apps/lab/build/outputs/apk/debug/lab-debug.apk
   ```

2. **Allow notifications for Space.** On Android 13+, Space requests `POST_NOTIFICATIONS` at first session start. Funtouch OS adds its own layer — check the system notification management settings for AetherPlay Space and make sure it is allowed to notify. If notifications are denied, sessions still run; you lose the visible session notification and its *Open game* / *Stop session* actions.
3. **Keep the session notification alive.** The session is a foreground service (type `specialUse`) with an ongoing notification. Aggressive notification management that hides or kills Space's notifications can make an active session hard to see or manage. Keep the notification channel enabled for Space.
4. **Avoid battery optimization killing Space during sessions.** Funtouch OS is aggressive about background process management. If a session dies unexpectedly, exempt AetherPlay Space from battery optimization in the system settings for the duration of your experiments. This protects the *host* process only — it is ordinary Android hygiene, not a special mode.
5. **Start from Space, connect from Lab.** As everywhere else: the session starts from Space's UI; Lab only attaches. See [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md).

## Pre-session checklist

Run through this before your first experiment on the device:

| Check | Expected state |
| --- | --- |
| Both apps installed from the same build | Settings shows both at version 0.1.0 (versionCode 1) |
| Notifications allowed for Space | Session notification visible after starting a session |
| Battery optimization exempted for Space (during sessions) | Session survives screen-off and app switching |
| Session started before opening Lab | Lab connects rather than reporting "no active session" |

## What specifically is unvalidated on this device

To be precise about the gap, none of the following has been observed on an iQOO Neo 6 (or any physical device):

- The Lab-to-Space bridge over real Binder.
- Session lifecycle behavior (start, stop, notification actions) under Funtouch OS notification management.
- Whether battery optimization kills Space's foreground service mid-session under typical settings.
- Notification-permission flow and its denial path on this OS skin.
- Any performance characteristic whatsoever — no measurements exist anywhere yet ([PERFORMANCE.md](PERFORMANCE.md)).

## A note on 120 Hz

The host UI (Compose, Material 3) is refresh-rate agnostic, and no 120 Hz-specific testing has been done. No special setting is expected to be needed; no 120 Hz claims are made.

## If something misbehaves

Work through [TROUBLESHOOTING.md](TROUBLESHOOTING.md) first — most issues (signature mismatch from mixed builds, notification denial, battery optimization) are device-agnostic. If it looks Neo 6-specific, capture logs with `adb logcat` while reproducing the problem; that output is the most useful thing a report can include. Attach the AetherPlay Space and AetherPlay Lab versions (both 0.1.0 in this build) to any report.

## Honest summary

| Question | Answer |
| --- | --- |
| Validated on a real iQOO Neo 6? | No. Physical validation pending. |
| Root required? | No — and root mode does not exist in AetherPlay. |
| Special Funtouch OS steps expected? | Notification allowance and battery-optimization exemption for Space. |
| 120 Hz tested? | No. UI is refresh-rate agnostic; no 120 Hz-specific testing done. |
