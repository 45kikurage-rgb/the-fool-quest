#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_JAR:?Set ANDROID_JAR to Android 35 platform android.jar}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS to SDK build-tools directory}"
: "${WIDGET_JAVAC:=javac}"
: "${SIGNING_STORE:?Set SIGNING_STORE to private fixed keystore}"
: "${SIGNING_STORE_PASSWORD:?Set SIGNING_STORE_PASSWORD}"
mkdir -p build/classes build/generated build/dex build/out
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$ANDROID_BUILD_TOOLS/aapt2" link -o build/out/resources.apk --manifest app/src/main/AndroidManifest.xml -I "$ANDROID_JAR" --java build/generated build/resources.zip
find app/src/main/java build/generated -name '*.java' -print > build/sources.txt
"$WIDGET_JAVAC" --release 8 -encoding UTF-8 -classpath "$ANDROID_JAR" -d build/classes @build/sources.txt
find build/classes -name '*.class' -print > build/classes.txt
java -cp "$ANDROID_BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$ANDROID_JAR" --output build/dex @build/classes.txt
cp build/out/resources.apk build/out/uncompressed.apk
(cd build/dex && zip -q -j ../out/uncompressed.apk classes*.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f -p 4 build/out/uncompressed.apk build/out/aligned.apk
java -jar "$ANDROID_BUILD_TOOLS/lib/apksigner.jar" sign --ks "$SIGNING_STORE" --ks-key-alias foolquestwidget --ks-pass env:SIGNING_STORE_PASSWORD --key-pass env:SIGNING_STORE_PASSWORD --out build/out/fool-quest-widget-0.1.7.apk build/out/aligned.apk
java -jar "$ANDROID_BUILD_TOOLS/lib/apksigner.jar" verify --verbose --print-certs build/out/fool-quest-widget-0.1.7.apk > build/signature-verification.txt
sha256sum build/out/fool-quest-widget-0.1.7.apk > build/SHA256SUMS
printf 'Signed APK built and verified.\n'
