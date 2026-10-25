# Privacy

AetherPlay is fully local. This page states exactly what that means, what data exists, where it lives, and how to delete all of it.

## No network, at all

Neither app's manifest requests the INTERNET permission. Without it, the operating system itself prevents either app from using the network. There are no accounts, no analytics, no crash reporting, no telemetry, no cloud sync, no ads, and no third-party SDKs that could add any of these back. Neither app can phone home even if it wanted to — and it does not.

| Question | Answer |
| --- | --- |
| Network access | None. The INTERNET permission is absent from both manifests. |
| Accounts | None. Nothing to sign into. |
| Analytics / telemetry / crash reporting | None. |
| Cloud storage or sync | None. All data is on-device. |
| Third-party data-sharing SDKs | None. |

## Permissions, and why

The complete permission set, from the manifests — these are all of them:

| App | Permission | Why |
| --- | --- | --- |
| Space | `POST_NOTIFICATIONS` (Android 13+) | Shows the ongoing session notification. Denied? The session still runs; you just lose the visible notification. |
| Space | `FOREGROUND_SERVICE` | Hosts the session as a foreground service. |
| Space | `FOREGROUND_SERVICE_SPECIAL_USE` | Declares the session service's `specialUse` type. |
| Space | declares `io.github.siddhantpardeshi.aetherplay.BRIDGE` (signature-level, custom) | Guards the bridge so only same-key apps (i.e., Lab) may connect. |
| Lab | `io.github.siddhantpardeshi.aetherplay.BRIDGE` (uses) | What Lab presents to authenticate to Space's bridge. |

No location, no contacts, no storage beyond each app's private sandbox, no anything else. You can verify this yourself by inspecting `apps/lab/src/main/AndroidManifest.xml` and `apps/space/src/main/AndroidManifest.xml`, or any released APK with `aapt dump permissions`.

## What data exists, and where

All data is stored in each app's private storage, invisible to other apps under the Android sandbox:

| Data | Owner | Format | Contents |
| --- | --- | --- | --- |
| Space metadata | Space | JSON files (app-private) | Named spaces and their settings. |
| Crystal Sprint saves | Space | Files (app-private) | Per-space field values: crystals, energy, best, speed, gems, tokens, runTime. |
| Saved experiments | Lab | JSON (app-private) | Search recipes: name, type, mode, value(s), alignment, regions. |

Session-scoped data — search results, the change journal, freezes — lives in memory only and disappears when the session stops.

## Data lifecycle

| Event | What happens to your data |
| --- | --- |
| Session stops | In-memory session data (results, journal, freezes) is gone. Saved fields persist. |
| Delete a space | That space's save files are permanently deleted (after a confirmation warning). |
| Uninstall either app | That app's private storage — all of its data — is removed with it. |
| Share an experiment | Only if you explicitly use share/export: a JSON file leaves the app because you handed it over. |

## Deletion

- **One experiment**: delete it in Lab's More tab (experiments).
- **One space and its save**: delete the space in Space (confirmation required). Permanent — there is no cloud copy to restore.
- **Everything an app holds**: uninstall the app; Android removes its private storage with it.
- **Share deliberately**: experiment export/share produces a JSON file you control. AetherPlay never sends anything anywhere on its own.

## What AetherPlay does not do to other apps

AetherPlay does not read other apps' memory. It works against the memory surface its own training game exposes inside Space (see [ARCHITECTURE.md](ARCHITECTURE.md)). It contains no root functionality and no anti-cheat evasion, ever. It does not and will not target online, competitive, or server-authoritative games.

## About the reference APK

During early research, the publicly distributed APK "GameGuardian 101.4" was **statically inspected only** — never installed, never run, and never granted anything. Nothing was copied from it: no code, no assets, no branding. It is not committed to this repository. AetherPlay's scope, design, and code are original. See [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

## Diagnostics

Diagnostics are manual and stay on your machine: if you report an issue, you capture logs yourself with `adb logcat` ([TROUBLESHOOTING.md](TROUBLESHOOTING.md)) and choose what to share. AetherPlay collects and transmits nothing automatically.
