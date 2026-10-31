#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${WIDGET_JAVAC:=javac}"
mkdir -p build/test
"$WIDGET_JAVAC" --release 8 -d build/test app/src/main/java/com/aruno/foolquest/widget/next/RevenueMath.java app/src/main/java/com/aruno/foolquest/widget/next/WidgetLayout.java tests/RevenueMathTest.java app/src/main/java/com/aruno/foolquest/widget/next/WidgetSpacing.java tests/WidgetSpacingTest.java app/src/main/java/com/aruno/foolquest/widget/next/RevenueHttp.java app/src/main/java/com/aruno/foolquest/widget/next/RevenueFailure.java tests/RevenueHttpTest.java
java -cp build/test WidgetSpacingTest
java -cp build/test RevenueMathTest
java -cp build/test com.aruno.foolquest.widget.next.RevenueHttpTest
node --test tests/export.test.cjs

