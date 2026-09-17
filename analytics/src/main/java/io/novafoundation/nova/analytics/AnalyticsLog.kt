package io.novafoundation.nova.analytics

import android.util.Log

private const val TAG = "NovaAnalytics"

/*
 * Debug-only trace of analytics delivery, filterable with `adb logcat -s NovaAnalytics`.
 *
 * Logs event names and counts, never event properties or the install id - enough to see whether
 * events are being dropped, queued or delivered, and what the backend answered.
 */

fun analyticsLog(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}

fun analyticsWarn(message: String) {
    if (BuildConfig.DEBUG) Log.w(TAG, message)
}
