#!/usr/bin/env bash
set -euo pipefail
adb shell wm size 390x844
adb shell wm density 160
adb shell settings put system font_scale 1.0
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 - <<'PY'
import json, xml.etree.ElementTree as E
root=E.Element('map')
def string(key, value):
    node=E.SubElement(root,'string',{'name':key}); node.text=value
string('display_name','Punith')
string('upi','yourname@upi')
string('dues',json.dumps([
 {'id':'1','name':'Ananya','amount':'500.00','reason':'Dinner at Toit','date':'2026-11-24','phone':'','settled':False,'lastNudged':0},
 {'id':'2','name':'Rahul','amount':'1250.00','reason':'Weekend trip','date':'2026-11-28','phone':'','settled':False,'lastNudged':0},
 {'id':'3','name':'Priya','amount':'600.00','reason':'Movie tickets','date':'2026-11-30','phone':'','settled':False,'lastNudged':0},
 {'id':'4','name':'Sam','amount':'800.00','reason':'Lunch','date':'2026-11-20','phone':'','settled':True,'lastNudged':0}]))
E.ElementTree(root).write('/tmp/awkward_free.xml',encoding='utf-8',xml_declaration=True)
PY
adb push /tmp/awkward_free.xml /data/local/tmp/awkward_free.xml
adb shell run-as com.punith.awkwardfree mkdir -p shared_prefs
adb shell run-as com.punith.awkwardfree cp /data/local/tmp/awkward_free.xml shared_prefs/awkward_free.xml
adb shell am force-stop com.punith.awkwardfree
adb shell am start -n com.punith.awkwardfree/.MainActivity
sleep 8
mkdir -p screenshots
adb exec-out screencap -p > screenshots/home.png
adb shell input tap 108 438
sleep 3
adb exec-out screencap -p > screenshots/detail.png
adb shell input tap 145 477
sleep 3
adb exec-out screencap -p > screenshots/nudge.png
adb shell input tap 195 805
sleep 3
adb exec-out screencap -p > screenshots/add.png
adb shell input tap 118 805
sleep 2
adb exec-out screencap -p > screenshots/dues.png
adb shell input tap 272 805
sleep 2
adb exec-out screencap -p > screenshots/settled.png
adb shell input tap 349 805
sleep 2
adb exec-out screencap -p > screenshots/settings.png
