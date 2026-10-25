#!/usr/bin/env python3
"""Generates the AetherPlay launcher icons (Lab mint / Space violet).

Outputs, per app:
  res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png  (legacy, API 23-25)
  res/mipmap-anydpi-v26/ic_launcher.xml                         (adaptive, API 26+)
  res/drawable/ic_launcher_fg.xml                               (vector foreground)

Run from anywhere: python scripts/generate_icons.py
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

LAB = {"start": (101, 227, 190), "end": (62, 201, 196), "bg": (11, 13, 18)}
SPACE = {"start": (181, 161, 255), "end": (127, 155, 255), "bg": (11, 13, 18)}

FG_VECTOR = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group android:scaleX="0.55" android:scaleY="0.55" android:pivotX="54" android:pivotY="54">
        <path
            android:fillColor="#{c1}"
            android:pathData="M54,14 L88,44 L54,96 L20,44 Z" />
        <path
            android:fillColor="#{c2}"
            android:pathData="M40,44 L54,30 L68,44 L54,58 Z" />
        <path
            android:fillColor="#{c1}"
            android:pathData="M31,46.5 L46.5,62 L54,82 L31,46.5 Z" />
    </group>
</vector>
"""

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_bg" />
    <foreground android:drawable="@drawable/ic_launcher_fg" />
</adaptive-icon>
"""

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def make_icon(size, colors):
    s = 8  # supersample
    img = Image.new("RGBA", (size * s, size * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # rounded dark tile
    r = int(size * s * 0.22)
    d.rounded_rectangle([0, 0, size * s - 1, size * s - 1], radius=r, fill=colors["bg"] + (255,))

    # gradient diamond via per-row polygons
    cx, cy = size * s / 2, size * s / 2
    w = size * s * 0.30
    top, bottom = cy - size * s * 0.36, cy + size * s * 0.36
    steps = 120
    prev = None
    for i in range(steps + 1):
        t = i / steps
        y = top + (bottom - top) * t
        half = w * (1 - abs(t - 0.5) * 0.1) * (0.35 + 0.65 * (1 - abs(t - 0.35)))
        col = lerp(colors["start"], colors["end"], t)
        poly = [(cx - half * 0.72, y), (cx, y + (bottom - top) / steps * 1.6), (cx + half * 0.72, y), (cx, y - (bottom - top) / steps * 1.6)]
        if prev is not None:
            d.polygon(prev + [poly[2], poly[1]], fill=col + (255,))
        prev = poly

    # facet highlight
    d.polygon([(cx - w * 0.5, cy - size * s * 0.02), (cx, cy - size * s * 0.20), (cx + w * 0.5, cy - size * s * 0.02), (cx, cy + size * s * 0.16)], fill=(255, 255, 255, 52))

    return img.resize((size, size), Image.LANCZOS)

def write_app(app, colors, hexes):
    base = os.path.join(ROOT, "apps", app, "src", "main", "res")
    for dens, size in DENSITIES.items():
        p = os.path.join(base, f"mipmap-{dens}")
        os.makedirs(p, exist_ok=True)
        make_icon(size, colors).save(os.path.join(p, "ic_launcher.png"))
    os.makedirs(os.path.join(base, "mipmap-anydpi-v26"), exist_ok=True)
    with open(os.path.join(base, "mipmap-anydpi-v26", "ic_launcher.xml"), "w") as f:
        f.write(ADAPTIVE)
    os.makedirs(os.path.join(base, "drawable"), exist_ok=True)
    with open(os.path.join(base, "drawable", "ic_launcher_fg.xml"), "w") as f:
        f.write(FG_VECTOR.format(c1=hexes[0], c2=hexes[1]))
    os.makedirs(os.path.join(base, "values"), exist_ok=True)
    with open(os.path.join(base, "values", "ic_launcher_colors.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_bg">#0B0D12</color>\n</resources>\n')
    print(f"{app}: icons written")

if __name__ == "__main__":
    write_app("lab", LAB, ("65E3BE", "F2F5FA"))
    write_app("space", SPACE, ("B5A1FF", "F2F5FA"))
