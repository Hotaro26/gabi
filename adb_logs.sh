#!/usr/bin/env bash
# Real-time Gabi ADB log monitor and diagnostic viewer

DEVICE=""
if [ -n "$1" ] && [ "$1" != "--crash" ] && [ "$1" != "--clear" ]; then
    DEVICE="-s $1"
    shift
fi

if [ "$1" == "--clear" ]; then
    echo "Clearing logcat..."
    adb $DEVICE logcat -c
    echo "Logcat cleared."
    exit 0
fi

if [ "$1" == "--crash" ]; then
    echo "Fetching last crash report from device..."
    adb $DEVICE shell "run-as com.material.downloader cat files/crash_dump.txt 2>/dev/null || cat /data/data/com.material.downloader/files/crash_dump.txt 2>/dev/null"
    exit 0
fi

echo "=========================================================="
echo " Streaming Gabi logs (Filter: GabiApp, AndroidRuntime, Python)"
echo " Press Ctrl+C to stop."
echo "=========================================================="

adb $DEVICE logcat -v time GabiApp:V AndroidRuntime:E Python:V FATAL:E *:S
