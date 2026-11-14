#!/usr/bin/env bash
set -uo pipefail
chmod +x gradlew
result=0
./gradlew connectedDebugAndroidTest --no-daemon || result=$?
# Conservar las capturas parciales incluso cuando una aserción falla.
adb pull /sdcard/Download/dlebox-check visual-check || true
exit "$result"
