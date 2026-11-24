#!/usr/bin/env bash
set -euo pipefail
package_name=com.punith.awkwardfree
bash ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear "$package_name" >/dev/null
adb shell run-as "$package_name" mkdir -p shared_prefs
printf '%s' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><boolean name="complete" value="true" /></map>' | adb shell run-as "$package_name" sh -c 'cat > shared_prefs/awkward_free_onboarding.xml'
adb shell am force-stop "$package_name"
adb shell am start -n "$package_name/.MainActivity" >/dev/null
sleep 6
mkdir -p visual-preview
adb exec-out screencap -p > visual-preview/dues.png
python3 - <<'PY'
import re
import subprocess
import time
import xml.etree.ElementTree as ET

def run(*args):
    return subprocess.run(args, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout

def select(description, output):
    run("adb", "shell", "uiautomator", "dump", "/sdcard/window.xml")
    root = ET.fromstring(run("adb", "exec-out", "cat", "/sdcard/window.xml"))
    match = None
    for node in root.iter("node"):
        if description in node.attrib.get("content-desc", "").split(", "):
            match = re.findall(r"\d+", node.attrib["bounds"])
            break
    if not match or len(match) != 4:
        raise RuntimeError("Cannot find navigation item: " + description)
    x1, y1, x2, y2 = map(int, match)
    run("adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(2)
    with open(output, "wb") as file:
        file.write(run("adb", "exec-out", "screencap", "-p"))

select("Friends", "visual-preview/friends.png")
select("Settings", "visual-preview/settings.png")
select("Add a due", "visual-preview/add.png")
PY
