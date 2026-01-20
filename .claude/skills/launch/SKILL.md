---
name: launch
description: Launch the app on connected device
disable-model-invocation: true
---

Launch the app on connected device via ADB:

1. Set environment:
   - ANDROID_HOME=/home/jinchan/android-sdk
   - ADB_SERVER_SOCKET=tcp:$(ip route | grep default | awk '{print $3}'):5037

2. Launch: `$ANDROID_HOME/platform-tools/adb shell am start -n com.portfolio.manager/.MainActivity`
