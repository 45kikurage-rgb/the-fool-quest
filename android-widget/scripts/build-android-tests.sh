#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_JAR:?}"; : "${ANDROID_BUILD_TOOLS:?}"; : "${WIDGET_JAVAC:=javac}"
mkdir -p build/android-test/classes build/android-test/dex
"$ANDROID_BUILD_TOOLS/aapt2" link -o build/android-test/base.apk --manifest tests/android/AndroidManifest.xml -I "$ANDROID_JAR"
"$WIDGET_JAVAC" --release 8 -encoding UTF-8 -classpath "$ANDROID_JAR:build/classes" -d build/android-test/classes tests/android/WidgetInstrumentation.java
find build/android-test/classes -name '*.class' -print > build/android-test/classes.txt
java -cp "$ANDROID_BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$ANDROID_JAR" --classpath build/classes --output build/android-test/dex @build/android-test/classes.txt
cp build/android-test/base.apk build/android-test/tests.apk
(cd build/android-test/dex && zip -q -j ../tests.apk classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 build/android-test/tests.apk build/android-test/aligned.apk
java -jar "$ANDROID_BUILD_TOOLS/lib/apksigner.jar" sign --ks "$SIGNING_STORE" --ks-key-alias foolquestwidget --ks-pass env:SIGNING_STORE_PASSWORD --key-pass env:SIGNING_STORE_PASSWORD --out build/android-test/widget-tests.apk build/android-test/aligned.apk
