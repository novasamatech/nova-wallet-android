package io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage

import kotlin.time.Duration.Companion.milliseconds

class RaiseAuthToken(
    val token: String,
    val expiresAt: Long
) {

    fun hasExpired(): Boolean {
        val currentTime = System.currentTimeMillis().milliseconds.inWholeSeconds
        return currentTime >= expiresAt
    }
}
