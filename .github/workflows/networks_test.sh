#!/usr/bin/env bash
adb devices
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.novafoundation.nova.balances.BalancesIntegrationTest