#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${WIDGET_JAVAC:=javac}"
mkdir -p build/test
"$WIDGET_JAVAC" --release 8 -d build/test app/src/main/java/com/aruno/foolquest/widget/next/RevenueMath.java app/src/main/java/com/aruno/foolquest/widget/next/WidgetLayout.java tests/RevenueMathTest.java app/src/main/java/com/aruno/foolquest/widget/next/WidgetSpacing.java tests/WidgetSpacingTest.java app/src/main/java/com/aruno/foolquest/widget/next/RevenueHttp.java app/src/main/java/com/aruno/foolquest/widget/next/RevenueFailure.java tests/RevenueHttpTest.java
java -cp build/test WidgetSpacingTest
"$WIDGET_JAVAC" --release 8 -encoding UTF-8 -d build/test app/src/main/java/com/aruno/foolquest/widget/next/CalendarDateMath.java tests/CalendarDateMathTest.java
java -cp build/test CalendarDateMathTest
"$WIDGET_JAVAC" --release 8 -encoding UTF-8 -classpath build/test -d build/test app/src/main/java/com/aruno/foolquest/widget/next/VatonRevenueCsv.java tests/VatonRevenueCsvTest.java
java -cp build/test com.aruno.foolquest.widget.next.VatonRevenueCsvTest
"$WIDGET_JAVAC" --release 8 -encoding UTF-8 -d build/test app/src/main/java/com/aruno/foolquest/widget/next/CalendarAmountFormatter.java tests/CalendarAmountFormatterTest.java
java -cp build/test CalendarAmountFormatterTest
java -cp build/test RevenueMathTest
java -cp build/test com.aruno.foolquest.widget.next.RevenueHttpTest
node --test tests/export.test.cjs

