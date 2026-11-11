#!/usr/bin/env bash
set -euo pipefail
APP="$1"
API="$2"
LEVEL="$API"
if [[ "$API" == 37 ]]; then LEVEL=37.0; fi
PARTITION_MB=4096
if [[ "$API" == 37 ]]; then PARTITION_MB=6144; fi
IMAGE="system-images;android-$LEVEL;google_apis;x86_64"
sdkmanager "$IMAGE" emulator
sudo chmod 666 /dev/kvm
export ANDROID_AVD_HOME="$RUNNER_TEMP/icon-avds"
mkdir -p "$ANDROID_AVD_HOME" verification
echo no | avdmanager create avd --force --name iconverify --package "$IMAGE" --device pixel_2 --path "$RUNNER_TEMP/iconverify.avd"
cat >> "$RUNNER_TEMP/iconverify.avd/config.ini" <<'AVD'
hw.ramSize=4096
vm.heapSize=512
hw.cpu.ncore=4
disk.dataPartition.size=4G
AVD
printf 'avd.ini.encoding=UTF-8\npath=%s/iconverify.avd\ntarget=android-%s\n' "$RUNNER_TEMP" "$LEVEL" > "$ANDROID_AVD_HOME/iconverify.ini"
if [[ "$API" == 37 ]]; then sed -i 's/disk.dataPartition.size=4G/disk.dataPartition.size=6G/' "$RUNNER_TEMP/iconverify.avd/config.ini"; fi
adb start-server
"$ANDROID_HOME/emulator/emulator" -avd iconverify -no-window -gpu software -memory 4096 -cores 4 -partition-size "$PARTITION_MB" -no-snapshot -noaudio -no-boot-anim -no-metrics -camera-back none -camera-front none > verification/emulator.txt 2>&1 &
EMULATOR_PID=$!
trap 'timeout 10 adb logcat -d -v time > verification/logcat.txt 2>&1 || true; kill "$EMULATOR_PID" 2>/dev/null || true' EXIT
BOOTED=false
DEADLINE=$((SECONDS+360))
while (( SECONDS < DEADLINE )); do
 kill -0 "$EMULATOR_PID" 2>/dev/null || { tail -60 verification/emulator.txt; exit 1; }
 if [[ "$(timeout 5 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then BOOTED=true;break;fi
 sleep 2
done
[[ "$BOOTED" == true ]]
adb root
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
if [[ "$APP" == home ]]; then adb shell wm size 720x1280; adb shell wm density 320; else adb shell wm size 1080x1920; adb shell wm density 480; fi
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell am force-stop com.google.android.googlesdksetup || true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true
python3 release-tests/code39/verify.py "$APP" "$API"

