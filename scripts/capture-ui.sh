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
tap_text() {
    local text="$1"
    adb shell uiautomator dump /sdcard/window.xml >/dev/null
    adb pull /sdcard/window.xml /tmp/awkward-window.xml >/dev/null
    local coords
    coords=$(python3 - "$text" <<'PYCODE'
import sys, re, xml.etree.ElementTree as E
root=E.parse('/tmp/awkward-window.xml')
for node in root.iter('node'):
    if node.get('text') == sys.argv[1]:
        a,b,c,d=map(int,re.findall(r'\d+',node.get('bounds')))
        print((a+c)//2,(b+d)//2)
        break
else: sys.exit('Text not found: '+sys.argv[1])
PYCODE
    )
    adb shell input tap $coords
}
tap_text 'Ananya'
sleep 3
adb exec-out screencap -p > screenshots/detail.png
tap_text 'Send a nudge'
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
# Exercise partial payment and confirm that the displayed balance is updated.
adb shell input tap 49 805
sleep 2
tap_text 'Ananya'
sleep 2
tap_text 'Record a payment'
sleep 2
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml /tmp/awkward-window.xml >/dev/null
coords=$(python3 - <<'PY'
import re, xml.etree.ElementTree as E
root=E.parse('/tmp/awkward-window.xml')
for node in root.iter('node'):
    if node.get('class') == 'android.widget.EditText':
        a,b,c,d=map(int,re.findall(r'\d+',node.get('bounds')))
        print((a+c)//2,(b+d)//2)
        break
else: raise SystemExit('Payment input not found')
PY
)
adb shell input tap $coords
adb shell input text 300
adb shell input keyevent 4
sleep 1
tap_text 'Record'
sleep 2
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml /tmp/awkward-window.xml >/dev/null
python3 - <<'PY'
import xml.etree.ElementTree as E
values=[n.get('text') for n in E.parse('/tmp/awkward-window.xml').iter('node')]
assert '₹200' in values, f'Remaining balance not shown: {values}'
PY
