#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${WIDGET_JAVAC:=javac}"
mkdir -p build/test
"$WIDGET_JAVAC" --release 8 -d build/test app/src/main/java/com/aruno/foolquest/widget/RevenueMath.java app/src/main/java/com/aruno/foolquest/widget/WidgetLayout.java tests/RevenueMathTest.java app/src/main/java/com/aruno/foolquest/widget/RevenueHttp.java app/src/main/java/com/aruno/foolquest/widget/RevenueFailure.java tests/RevenueHttpTest.java
java -cp build/test RevenueMathTest
java -cp build/test com.aruno.foolquest.widget.RevenueHttpTest
node --test tests/export.test.cjs
