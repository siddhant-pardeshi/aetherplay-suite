# Release and Signing

The bridge between Lab and Space is guarded by a signature-level permission, so signing is not a formality here: **both apps in any installed pair must be signed with the same key.** This page covers the debug default, the release procedure, checksums, and release hygiene.

## Why the same key is mandatory

Space's bridge service is protected by the signature-level permission `io.github.siddhantpardeshi.aetherplay.BRIDGE`. Android grants a signature permission only to apps signed with the same key as the app that declared it. Consequences:

- Lab and Space signed with the same key: the bridge authenticates and everything works.
- A debug Lab against a release Space (or any mixed pair): the permission is not granted, Lab cannot bind, and you get the "Space is not connected" / signature-mismatch symptoms in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
- This is a security feature, not an inconvenience: it is what stops an unrelated app from pretending to be Lab.

## Debug builds: works out of the box

Paired debug builds share the machine's debug keystore, so both APKs from one build machine are automatically signed identically:

```bash
./gradlew :apps:lab:assembleDebug :apps:space:assembleDebug
adb install -r apps/space/build/outputs/apk/debug/space-debug.apk
adb install -r apps/lab/build/outputs/apk/debug/lab-debug.apk
```

Caveat: two different machines usually have two different debug keystores. Build the pair on one machine, or share a keystore deliberately.

## Release builds

### 1. Create one keystore

```bash
keytool -genkeypair -v -keystore aetherplay-release.keystore -alias aetherplay \
  -keyalg RSA -keysize 4096 -validity 10000
```

You will use this **one** keystore for **both** apps. Losing it means future updates can never be installed over existing installs — back it up somewhere safe and offline.

### 2. Wire it in via an uncommitted properties file

Keep secrets out of the repository. Use a local file, for example `keystore.properties`:

```properties
storeFile=../aetherplay-release.keystore
storePassword=...
keyAlias=aetherplay
keyPassword=...
```

**Note, honestly:** as of this build, no release signing config is wired into either app's `build.gradle.kts` (the `release` block enables minification only), and the repository `.gitignore` does not yet cover keystore files. You must add both yourself: a `signingConfigs` block in **each** of `apps/lab` and `apps/space` that reads the same `keystore.properties` (guarding on file existence so clean clones without the file still build), plus `.gitignore` entries for `keystore.properties` and `*.keystore` before your first release commit. Wire both modules to the same file so both release APKs are signed with the same key by construction.

### 3. Build both release APKs

```bash
./gradlew :apps:lab:assembleRelease :apps:space:assembleRelease
```

Outputs land in `apps/lab/build/outputs/apk/release/` and `apps/space/build/outputs/apk/release/`.

### 4. Verify the pair before shipping

Confirm both APKs really do share a signer on your test device, and that the bridge connects, **before** publishing. Until signing is wired in (step 2), outputs are the default unsigned artifacts:

```bash
adb install -r apps/space/build/outputs/apk/release/space-release-unsigned.apk
adb install -r apps/lab/build/outputs/apk/release/lab-release-unsigned.apk
```

(With signing configured, the names become `space-release.apk` / `lab-release.apk`.) Unsigned APKs will not install with `adb install -r` on modern Android — sign them first, then verify: if Lab connects to Space after installing both, the pair is matched. If it does not, stop and fix signing — do not publish.

## Checksums for release assets

Publish SHA-256 checksums alongside every APK so users can verify downloads:

```bash
sha256sum space-release.apk lab-release.apk
```

(Windows PowerShell: `Get-FileHash <apk> -Algorithm SHA256`.)

## GitHub Releases asset naming

Publish to `siddhant-pardeshi/aetherplay-suite` releases with names that state app, version, and build type unambiguously, e.g.:

| Asset | Suggested name |
| --- | --- |
| Space release APK | `aetherplay-space-v0.1.0.apk` |
| Lab release APK | `aetherplay-lab-v0.1.0.apk` |

Always publish Lab and Space of the same version together, and say so in the release notes: a matched pair is a requirement, not a recommendation. Include both checksums in the release notes.

## Mapping and symbol retention

Retain the release build outputs you will need to read crash stack traces later:

- R8 mapping files (`mapping.txt` under each module's `build/outputs/mapping/release/`) — keep per release, privately; symbolication of any future stack trace depends on them.
- Keep them out of the public repo if they are large, but archive them indexed by versionCode (1 in this build) so the right mapping is always findable.

## Hygiene rules

- **Never commit keystores or password files.** The repository `.gitignore` does not yet cover them — add entries for `keystore.properties` and `*.keystore` before your first release, and re-check before every release after that.
- **One key for both apps, forever.** Changing the key later breaks update paths for both apps simultaneously.
- **Version both apps together.** versionName 0.1.0 / versionCode 1 in this build; bump both in lockstep so the protocol version (1.0) is always paired with a coherent release.
- **Test the pair before publishing** — a mixed-signing pair fails only at connect time, which is exactly what users will report if you skip step 4.
