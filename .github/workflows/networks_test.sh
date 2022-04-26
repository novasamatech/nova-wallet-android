#!/usr/bin/env bash
adb devices
# adb logcat -c
# adb logcat &
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.novafoundation.nova.balances.BalancesIntegrationTest

# download results
adb exec-out run-as io.novafoundation.nova.debug sh -c 'cd /data/data/io.novafoundation.nova.debug/files && tar cf - allure-results' > allure-results.tar