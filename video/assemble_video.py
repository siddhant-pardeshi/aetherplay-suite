#!/usr/bin/env python3
"""Assemble the AetherPlay walkthrough video: animated slides + real emulator
footage, narrated end-to-end. Visual mapping lives in VISUALS below."""
import json, subprocess, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
os.chdir(HERE)

def run(cmd):
    print(">>", " ".join(cmd[:8]), "...")
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[-3000:])
        sys.exit(1)

def dur(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                        "-of", "csv=p=0", path], capture_output=True, text=True)
    return float(r.stdout.strip())

script = json.load(open("script.json", encoding="utf-8"))

# narration id -> visual source: ("slide", None) or ("live", (file, start_offset))
VISUALS = {
    "s01_intro":        ("slide", None),
    "s02_pair":         ("slide", None),
    "s03_setup":        ("slide", None),
    "s04_session":      ("live", ("rec_f", 5)),     # session start + running (Gen 2 era)
    "s05_game":         ("live", ("rec_e", 30)),    # crystal sprint gameplay
    "s06_search":       ("live", ("rec_f", 95)),    # Lab search results
    "s07_refine":       ("live", ("rec_c", 20)),    # refine increased 121 -> 3
    "s08_write":        ("live", ("rec_f", 150)),   # preview + commit + journal
    "s09_freeze":       ("live", ("rec_g", 40)),    # freeze holds in game
    "s10_spaces":       ("slide", None),
    "s11_experiments":  ("slide", None),
    "s12_honest":       ("slide", None),
    "s13_outro":        ("slide", None),
}

FPS = 30
TAIL = 0.9  # extra seconds of visual per segment after narration

inputs = []
filters = []
vstreams = []
astreams = []

seg_index = 0
for seg in script["segments"]:
    sid = seg["id"]
    wav = f"voice/{sid}.wav"
    ad = dur(wav) + TAIL
    frames = int(ad * FPS) + 2
    kind, spec = VISUALS[sid]

    if kind == "slide":
        slide_png = f"frames/{sid}.png"
        if not os.path.exists(slide_png):
            # fall back to any slide (e.g. renamed) — keeps assembly robust
            slide_png = f"frames/{sid.split('_')[0]}_{sid.split('_')[1]}.png"
        inputs += ["-loop", "1", "-t", f"{ad:.2f}", "-i", slide_png]
        filters.append(
            f"[{seg_index}:v]scale=1920:1080,zoompan=z='1+0.00045*on':"
            f"x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1920x1080:fps={FPS},"
            f"format=yuv420p[v{seg_index}]"
        )
    else:
        rec, offset = spec
        rec_path = f"recordings/{rec}.mp4"
        inputs += ["-i", rec_path]
        filters.append(
            f"[{seg_index}:v]trim=start={offset}:duration={ad:.2f},setpts=PTS-STARTPTS,"
            f"scale=-2:1080:force_original_aspect_ratio=decrease,"
            f"pad=1920:1080:(ow-iw)/2:(oh-ih)/2:color=0x0B0D12,"
            f"fps={FPS},tpad=stop_mode=clone:stop_duration=5,format=yuv420p[v{seg_index}]"
        )

    inputs += ["-i", wav]
    a_idx = seg_index + 1
    filters.append(f"[{a_idx}:a]apad=pad_dur={TAIL}[a{a_idx}]")
    vstreams.append(f"[v{seg_index}]")
    astreams.append(f"[a{a_idx}]")
    seg_index += 2

# concat video, concat audio, mux
n = seg_index
filters.append(
    f"{''.join(vstreams)}concat=n={len(vstreams)}:v=1:a=0[vout]"
)
filters.append(
    f"{''.join(astreams)}concat=n={len(astreams)}:v=0:a=1[aout]"
)

cmd = ["ffmpeg", "-y", *inputs, "-filter_complex", ";".join(filters),
       "-map", "[vout]", "-map", "[aout]",
       "-c:v", "libx264", "-preset", "medium", "-crf", "21",
       "-c:a", "aac", "-b:a", "160k", "-shortest",
       "AetherPlay-Walkthrough.mp4"]
run(cmd)
print("DONE", "AetherPlay-Walkthrough.mp4", f"{dur('AetherPlay-Walkthrough.mp4'):.1f}s")
