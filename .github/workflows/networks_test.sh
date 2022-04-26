#!/usr/bin/env bash
adb devices
# adb logcat -c
# adb logcat &

# Build tests
./gradlew :app:assembleDebugAndroidTest

# Run tests
adb shell am instrument -w -m -e debug false -e class 'io.novafoundation.nova.balances.BalancesIntegrationTest' io.novafoundation.nova.debug.test/io.qameta.allure.android.runners.AllureAndroidJUnitRunner
EXIT_STATUS=$?

# Export results
adb exec-out run-as io.novafoundation.nova.debug sh -c 'cd /data/data/io.novafoundation.nova.debug/files && tar cf - allure-results' > allure-results.tar

exit $EXIT_STATUS