# Performance

> **No measurements exist yet.** No physical device or emulator was available in the authoring session, so not a single number on this page has been observed. The budgets below are **targets from the design, not results**. Every cell in the results table is intentionally empty until measured on real hardware.

## Budgets (targets, not results)

| Metric | Budget (target) | Measured result |
| --- | --- | --- |
| Cold start, p95 | ≤ 2.0 s | not measured |
| Warm start, p95 | ≤ 1.0 s | not measured |
| Search cancel acknowledgment | ≤ 250 ms | not measured |
| Lab reconnect to a running session | ≤ 2 s | not measured |
| Idle session overhead | < 1% of one core | not measured |

## Known engineering bounds (facts, not measurements)

These are design constants, not performance observations:

- Search results are bounded at **100,000 candidates** and delivered in pages of **100** ("Load more"). The cap bounds memory and UI cost by construction.
- Freeze re-applies values on a **300 ms** loop while the session runs.
- The memory surface is intentionally tiny (a 64-byte writable `state` region and a 128-byte read-only `playfield` region), so scan size is not a meaningful variable in this build.

## How to measure, when you have a device

Run everything against **release** builds (`assembleRelease`, both apps signed with the same key — see [RELEASE_AND_SIGNING.md](RELEASE_AND_SIGNING.md)); debug builds carry overhead that will pollute every number.

### 1. Cold and warm start — `adb shell am start -W`

```bash
adb shell am start -W -n io.github.siddhantpardeshi.aetherplay.space/.MainActivity
adb shell am start -W -n io.github.siddhantpardeshi.aetherplay.lab/.MainActivity
```

`TotalTime` is your per-launch sample. Cold start: force-stop the app (`adb shell am force-stop <package>`) before each run. Warm start: relaunch without force-stopping. Take enough samples (30+) to report a p95, not an average.

### 2. Search cancel, freeze tick, session overhead — Perfetto

```bash
adb shell perfetto -o /data/misc/perfetto-traces/trace -t 30s \
  sched freq idle am wm gfx view
adb pull /data/misc/perfetto-traces/trace
```

Open the trace at `ui.perfetto.dev`. Look for: the cancel path's latency from cancel request to acknowledgment, the 300 ms freeze ticks' CPU cost, and Space's foreground-service CPU while a session idles with the game open.

### 3. Start-up and scroll as benchmarks — Macrobenchmark

Add a Macrobenchmark module (not present in this repo yet) with `StartupTimingMetric` for both apps, and drive a scripted search/refine interaction for frame and jank metrics. Keep the benchmark module out of the release path.

### 4. Idle overhead

With a session running and the game open, sample CPU over a few minutes:

```bash
adb shell top -m 10 | grep -E "aetherplay"
```

Report average and peak CPU for Space's process against the < 1% core target.

## Results table (to be filled on-device)

| Metric | Device / build | Date | Result | Budget met? |
| --- | --- | --- | --- | --- |
| Cold start p95 | | | | |
| Warm start p95 | | | | |
| Search cancel ack | | | | |
| Reconnect | | | | |
| Idle overhead | | | | |

When you fill a row, record the exact build (commit, debug or release, signing) and the device. A number without those three facts is not a result. See [STATUS.md](STATUS.md) for where device validation sits in the milestone plan.
