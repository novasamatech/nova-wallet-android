package io.novafoundation.nova.infrastructure.attestation

import android.util.Log
import io.novafoundation.nova.infrastructure.BuildConfig

private const val TAG = "NovaAttestation"

/*
 * Debug-only trace of the attestation handshake, filterable with `adb logcat -s NovaAttestation`.
 *
 * Deliberately never carries the shared secret, challenges, signatures, public keys or integrity
 * tokens - only what is needed to line the client's view up against the backend's logs: which host,
 * which client id, which HTTP status.
 *
 * Not called from invalidate(): the JVM unit tests exercise it, and android.util.Log is not mocked
 * there. The interceptor that calls it logs the recovery instead.
 */

internal fun attestationLog(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}

internal fun attestationWarn(message: String) {
    if (BuildConfig.DEBUG) Log.w(TAG, message)
}
