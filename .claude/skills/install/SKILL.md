---
name: install
description: Install debug APK to connected device
disable-model-invocation: true
---

Install the debug APK to connected device via ADB:

1. Set environment:
   - ANDROID_HOME=/home/jinchan/android-sdk
   - ADB_SERVER_SOCKET=tcp:$(ip route | grep default | awk '{print $3}'):5037

2. Install: `$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`
