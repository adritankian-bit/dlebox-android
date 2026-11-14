#!/usr/bin/env bash
set -euo pipefail

wait_for_catalog() {
  for attempt in $(seq 1 30); do
    adb shell uiautomator dump /sdcard/dlebox-ui.xml > /dev/null 2>&1 || true
    if adb exec-out cat /sdcard/dlebox-ui.xml 2>/dev/null | grep -q 'Descubre'; then
      return 0
    fi
    sleep 1
  done
  echo 'No apareció el catálogo antes del límite de captura' >&2
  adb logcat -d -s AndroidRuntime:E >&2
  return 1
}

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd uimode night no
adb shell am force-stop com.dlebox.android
adb shell am start -W -n com.dlebox.android/.MainActivity
wait_for_catalog
adb exec-out screencap -p > DleBox-Android35.png
adb shell cmd uimode night yes
wait_for_catalog
adb exec-out screencap -p > DleBox-Android35-dark.png
