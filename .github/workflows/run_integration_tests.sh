#!/usr/bin/env bash
adb devices

# Install debug app
adb -s emulator-5554 install app/androidTest/debug/app-debug-androidTest.apk

# Install instrumental tests
adb -s emulator-5554 install app/instrumentialTest/app-instrumentialTest.apk

export TEST_CLASSES=$@

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
test_classes = os.getenv("TEST_CLASSES")
run_command = f"adb shell am instrument -w -m -e debug false -e class {test_classes} io.novafoundation.nova.debug.test/io.qameta.allure.android.runners.AllureAndroidJUnitRunner"
def run():
  os.system('adb wait-for-device')
  p = sp.Popen(run_command,shell=True, stdout=sp.PIPE, stderr=sp.PIPE, stdin=sp.PIPE)
  return p.communicate()
success = re.compile(r'OK \(\d+ tests\)')
stdout, stderr = run()
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
