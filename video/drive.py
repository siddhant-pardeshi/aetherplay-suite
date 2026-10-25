#!/usr/bin/env python3
"""Tiny uiautomator helper for driving the AetherPlay demo on the emulator."""
import subprocess, sys, time, re, os

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")

def sh(*args, **kw):
    return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=60, **kw)

def dump():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = sh("shell", "cat", "/sdcard/ui.xml").stdout
    return xml

def find_bounds(pattern, xml=None):
    xml = xml or dump()
    # find node containing text/content-desc matching pattern; take its bounds
    for m in re.finditer(r'<node[^>]*?(?:text|content-desc)="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*?/>', xml):
        label = m.group(1)
        if re.search(pattern, label):
            x1, y1, x2, y2 = map(int, m.group(2, 3, 4, 5))
            return (x1 + x2) // 2, (y1 + y2) // 2
    return None

def tap(x, y, wait=0.8):
    sh("shell", "input", "tap", str(x), str(y))
    time.sleep(wait)

def tap_text(pattern, wait=1.0, required=True):
    b = find_bounds(pattern)
    if b is None:
        if required:
            print(f"NOT FOUND: {pattern}")
            return False
        return False
    print(f"tap {pattern!r} @ {b}")
    tap(*b, wait=wait)
    return True

def swipe(x1, y1, x2, y2, ms=300, wait=0.3):
    sh("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    time.sleep(wait)

def text(s, wait=0.5):
    sh("shell", "input", "text", s)
    time.sleep(wait)

def key(keycode, wait=0.4):
    sh("shell", "input", "keyevent", str(keycode))
    time.sleep(wait)

def screenshot(name):
    with open(name, "wb") as f:
        r = subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True)
        f.write(r.stdout)
    print(f"shot -> {name}")

def read_texts():
    xml = dump()
    return re.findall(r'(?:text|content-desc)="([^"]+)"', xml)

if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "tap":
        tap_text(sys.argv[2], wait=float(sys.argv[3]) if len(sys.argv) > 3 else 1.0)
    elif cmd == "texts":
        for t in read_texts():
            if t.strip():
                print(repr(t))
    elif cmd == "shot":
        screenshot(sys.argv[2])
    elif cmd == "text":
        text(sys.argv[2])
    elif cmd == "key":
        key(sys.argv[2])
    elif cmd == "swipe":
        swipe(*map(int, sys.argv[2:7]))
