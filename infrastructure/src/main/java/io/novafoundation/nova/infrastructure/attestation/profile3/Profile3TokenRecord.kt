package io.novafoundation.nova.infrastructure.attestation.profile3

import java.io.IOException
import java.security.SecureRandom
import kotlin.math.roundToLong

data class Profile3TokenScope(val origin: String, val clientId: String, val generation: Long) {

    init {
        require(origin.isNotBlank() && clientId.isNotBlank() && generation >= 0) { "Invalid token scope" }
    }
}

interface Profile3TokenClock {
    fun elapsedRealtimeMillis(): Long

    fun bootCount(): Int
}

fun interface Profile3TokenRandom {
    fun nextDouble(): Double
}

class SecureProfile3TokenRandom(private val random: SecureRandom) : Profile3TokenRandom {
    override fun nextDouble(): Double = random.nextDouble()
}

class Profile3TokenRecord internal constructor(
    val scope: Profile3TokenScope,
    val accessToken: String,
    val issuedAtSeconds: Long,
    val expiresAtSeconds: Long,
    val issuanceStartElapsedRealtimeMillis: Long,
    val bootCount: Int,
    val earlyRefreshWindowSeconds: Long
) {

    val deadlineElapsedRealtimeMillis: Long

    val refreshAtElapsedRealtimeMillis: Long

    init {
        require(accessToken.isNotEmpty() && accessToken.length <= MAX_TOKEN_LENGTH && accessToken.all { it.code in 33..126 }) {
            "Invalid access token"
        }
        val lifetime = requireLifetime(issuedAtSeconds, expiresAtSeconds)
        require(bootCount >= 0 && issuanceStartElapsedRealtimeMillis >= 0) { "Invalid token clock context" }
        val usableMillis = lifetime * 1_000L - DEADLINE_MARGIN_MILLIS
        require(issuanceStartElapsedRealtimeMillis <= Long.MAX_VALUE - usableMillis) { "Token deadline overflow" }
        require(earlyRefreshWindowSeconds in (lifetime / 5.0).roundToLong()..(lifetime * 4.0 / 15.0).roundToLong()) {
            "Invalid refresh window"
        }
        deadlineElapsedRealtimeMillis = issuanceStartElapsedRealtimeMillis + usableMillis
        refreshAtElapsedRealtimeMillis = deadlineElapsedRealtimeMillis - earlyRefreshWindowSeconds * 1_000L
    }

    fun isUsable(nowElapsedRealtimeMillis: Long, currentBootCount: Int): Boolean =
        bootCount == currentBootCount && nowElapsedRealtimeMillis >= issuanceStartElapsedRealtimeMillis &&
            nowElapsedRealtimeMillis < deadlineElapsedRealtimeMillis

    fun shouldRefresh(nowElapsedRealtimeMillis: Long): Boolean = nowElapsedRealtimeMillis >= refreshAtElapsedRealtimeMillis

    override fun toString(): String = "Profile3TokenRecord(<redacted>)"

    internal companion object {
        const val MAX_TOKEN_LENGTH = 3_072
        const val DEADLINE_MARGIN_MILLIS = 5_000L

        fun requireLifetime(issued: Long, expires: Long): Long {
            require(issued >= 0 && expires > issued) { "Invalid token timestamps" }
            val lifetime = expires - issued
            require(lifetime in 60L..900L) { "Invalid token lifetime" }
            return lifetime
        }
    }
}

class Profile3TokenRecordFactory(
    private val clock: Profile3TokenClock,
    private val random: Profile3TokenRandom
) {

    fun create(
        scope: Profile3TokenScope,
        accessToken: String,
        issuedAtSeconds: Long,
        expiresAtSeconds: Long,
        issuanceStartElapsedRealtimeMillis: Long,
        bootCount: Int
    ): Profile3TokenRecord {
        try {
            val lifetime = Profile3TokenRecord.requireLifetime(issuedAtSeconds, expiresAtSeconds)
            val sample = random.nextDouble()
            require(sample.isFinite() && sample in 0.0..1.0) { "Invalid refresh randomness" }
            val window = (lifetime / 5.0 + sample * lifetime / 15.0).roundToLong()
            val record = Profile3TokenRecord(
                scope,
                accessToken,
                issuedAtSeconds,
                expiresAtSeconds,
                issuanceStartElapsedRealtimeMillis,
                bootCount,
                window
            )
            require(record.isUsable(clock.elapsedRealtimeMillis(), clock.bootCount())) { "Token already expired or boot changed" }
            return record
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid profile-3 token record")
        }
    }
}
