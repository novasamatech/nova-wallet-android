#!/usr/bin/env bash
adb devices
adb logcat -c
adb logcat &
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.novafoundation.nova.balances.BalancesIntegrationTest