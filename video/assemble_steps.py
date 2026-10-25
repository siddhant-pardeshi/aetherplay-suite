#!/usr/bin/env python3
"""Assemble 'First Experiment, Step by Step' — step slides + v0.1.1 live footage."""
import json, subprocess, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
os.chdir(HERE)

def run(cmd):
    print(">>", " ".join(cmd[:6]), "...")
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[-3000:])
        sys.exit(1)

def dur(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                        "-of", "csv=p=0", path], capture_output=True, text=True)
    return float(r.stdout.strip())

script = json.load(open("script_steps.json", encoding="utf-8"))

# narration id -> ("slide", None) or ("live", (recording, offset))
VISUALS = {
    "st01_intro":   ("slide", None),
    "st02_install": ("slide", None),
    "st03_session": ("live", ("step_b", 8)),    # real Start session + running state
    "st04_play":    ("live", ("step_b", 55)),   # Crystal Sprint gameplay
    "st05_search":  ("live", ("step_c", 30)),   # unknown search, real values in rows
    "st06_write":   ("live", ("step_c", 95)),   # preview -> commit "Applied to 1 value"
    "st07_freeze":  ("live", ("step_c", 150)),  # freeze set + HUD 999 + holds
    "st08_scope":   ("slide", None),
    "st09_outro":   ("live", ("step_c", 162)),  # clean stop
}

FPS = 30
TAIL = 0.9

inputs, filters, vstreams, astreams = [], [], [], []
seg_index = 0
for seg in script["segments"]:
    sid = seg["id"]
    prefix = f"st{int(sid[2:4])}"  # st01_intro -> st1.png … st09_outro -> st9.png
    wav = f"voice_steps/{sid}.wav"
    ad = dur(wav) + TAIL
    kind, spec = VISUALS[sid]

    if kind == "slide":
        inputs += ["-loop", "1", "-t", f"{ad:.2f}", "-i", f"frames/{prefix}.png"]
        filters.append(
            f"[{seg_index}:v]scale=1920:1080,zoompan=z='1+0.00045*on':"
            f"x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1920x1080:fps={FPS},"
            f"format=yuv420p[v{seg_index}]"
        )
    else:
        rec, offset = spec
        inputs += ["-i", f"recordings/{rec}.mp4"]
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

filters.append(f"{''.join(vstreams)}concat=n={len(vstreams)}:v=1:a=0[vout]")
filters.append(f"{''.join(astreams)}concat=n={len(astreams)}:v=0:a=1[aout]")

run(["ffmpeg", "-y", *inputs, "-filter_complex", ";".join(filters),
     "-map", "[vout]", "-map", "[aout]",
     "-c:v", "libx264", "-preset", "medium", "-crf", "21",
     "-c:a", "aac", "-b:a", "160k", "-shortest",
     "AetherPlay-First-Experiment-Steps.mp4"])
print("DONE", f"{dur('AetherPlay-First-Experiment-Steps.mp4'):.1f}s")
