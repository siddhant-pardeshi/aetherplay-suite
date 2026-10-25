# AetherPlay Quickstart

AetherPlay is a two-app suite for learning how memory search, refinement, and bounded editing work — against its own offline training game, not other people's games. This page takes you from a clean clone to your first successful search in about five minutes of hands-on time.

The two apps, both versionName 0.1.0 (versionCode 1):

| App | Package | Role |
| --- | --- | --- |
| AetherPlay Lab | `io.github.siddhantpardeshi.aetherplay.lab` | The "instrument": search, refine, read, write, undo, freeze |
| AetherPlay Space | `io.github.siddhantpardeshi.aetherplay.space` | The "stage": named spaces, the session foreground service, and the Crystal Sprint training game |

## 1. What you need

- JDK 17.
- Android SDK with `platforms;android-37.0` and `build-tools;36.0.0` installed (build-tools is AGP's default).
- `adb` on your PATH.
- A device or emulator running Android 6.0 (API 23) or newer. Note: AetherPlay has **not been tested on any physical device yet** — see [STATUS.md](STATUS.md).
- About 5 minutes plus build time.

## 2. Clone

```bash
git clone https://github.com/siddhant-pardeshi/aetherplay-suite.git
cd aetherplay-suite
```

## 3. Build both apps

```bash
./gradlew :apps:lab:assembleDebug :apps:space:assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

| Artifact | Path |
| --- | --- |
| Lab debug APK | `apps/lab/build/outputs/apk/debug/lab-debug.apk` |
| Space debug APK | `apps/space/build/outputs/apk/debug/space-debug.apk` |

## 4. Install both APKs as a pair

```bash
adb install -r apps/space/build/outputs/apk/debug/space-debug.apk
adb install -r apps/lab/build/outputs/apk/debug/lab-debug.apk
```

Install **both**. The two apps talk over an authenticated bridge that requires both APKs to be signed with the same key. Paired debug builds work out of the box because they share the debug keystore. Never mix a debug build of one app with a release build of the other — the bridge will reject the connection. Details: [RELEASE_AND_SIGNING.md](RELEASE_AND_SIGNING.md).

## 5. Your first five minutes

1. **Open AetherPlay Space** on the device and start a session on the default space. On Android 13+ you will be asked for notification permission — allow it if you want the visible session notification; the session runs either way.
2. **Open Crystal Sprint** from Space (the notification's *Open game* action also works). Catch a few crystals and remember the count — say 5.
3. **Open AetherPlay Lab** and connect. Lab attaches to the already-running session; it cannot start one by design.
4. **Search**: type Int32, mode exact, value 5, alignment 4. You will get several results, not one — that is expected. The training game deliberately exposes mirrors of the crystal count alongside the real value. Walk through finding the real one in [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md).

## What works today, what does not

Works today: paired debug builds, the Lab-to-Space bridge protocol (1.0), search (exact/range/unknown over 10 value types), refinement, read, previewed write, last-write undo, bounded freeze, saved experiments, named spaces with separate saves.

Not yet: any third-party game support (there is no guest runtime in this build), scripts, pattern/pointer search, overlays, root mode — and none of it has been validated on a physical device. The full honest list is in [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).

## Next steps

- [TUTORIAL_01_FIRST_SESSION.md](TUTORIAL_01_FIRST_SESSION.md) — the full happy path: search, refine, write, undo, freeze.
- [USER_GUIDE.md](USER_GUIDE.md) — every screen in both apps.
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — when something does not connect or hold.
