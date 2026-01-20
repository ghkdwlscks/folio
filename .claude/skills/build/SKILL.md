---
name: build
description: Build and install debug APK
disable-model-invocation: true
---

Build and install the Android app:

1. Set environment:
   - JAVA_HOME=/home/jinchan/java/jdk-17.0.10
   - ANDROID_HOME=/home/jinchan/android-sdk

2. Build: `./gradlew assembleDebug`

3. Install via ADB:
   - Set ADB_SERVER_SOCKET=tcp:$(ip route | grep default | awk '{print $3}'):5037
   - Run: $ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
