#!/usr/bin/env bash
adb devices

# Install debug app
adb -s emulator-5554 install app/debug/app-debug.apk

# Install instrumental tests
adb -s emulator-5554 install test-app/androidTest/debug/app-debug-androidTest.apk

# Run tests
adb logcat -c &&
python - <<END
import os
import re
import subprocess as sp
import sys
import threading
import time

done = False
def update():
  # prevent CI from killing the process for inactivity
  while not done:
    time.sleep(5)
    print ("Running...")
t = threading.Thread(target=update)
t.dameon = True
t.start()
def run():
  os.system('adb wait-for-device')
  p = sp.Popen('adb shell am instrument -w -m -e notClass io.novafoundation.nova.balances.BalancesIntegrationTest -e package io.novafoundation.nova.debug io.novafoundation.nova.debug.test/io.qameta.allure.android.runners.AllureAndroidJUnitRunner',
               shell=True, stdout=sp.PIPE, stderr=sp.PIPE, stdin=sp.PIPE)
  return p.communicate()
success = re.compile(r'OK \(\d+ tests\)')
stdout, stderr = run()
stdout = stdout.decode('ISO-8859-1')
stderr = stderr.decode('ISO-8859-1')
done = True
print (stderr)
print (stdout)
if success.search(stderr + stdout):
  sys.exit(0)
else:
  sys.exit(1) # make sure we fail if the tests fail
END
EXIT_CODE=$?
adb logcat -d '*:E'

# Export results
adb exec-out run-as io.novafoundation.nova.debug sh -c 'cd /data/data/io.novafoundation.nova.debug/files && tar cf - allure-results' > allure-results.tar

exit $EXIT_CODE
