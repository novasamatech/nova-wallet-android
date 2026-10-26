package io.novafoundation.nova.infrastructure.attestation.profile3

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Profile3TokenRecordTest {

    private val clock = TestTokenClock()

    private val random = Profile3TokenRandom { 0.0 }

    private val factory = Profile3TokenRecordFactory(clock, random)

    private val scope = Profile3TokenScope("https://infra.test", "client", 1)

    private val gson = Gson()

    @Test
    fun `deadline subtracts request duration and safety margin without using calendar time`() {
        clock.now = 20_000
        val record = factory.create(scope, "opaque", 100, 400, 1_000, clock.boot)
        assertEquals(296_000, record.deadlineElapsedRealtimeMillis)
        assertTrue(record.isUsable(295_999, clock.boot))
        assertFalse(record.isUsable(296_000, clock.boot))
        assertFalse(record.isUsable(2_000, clock.boot + 1))
        assertFalse(record.isUsable(999, clock.boot))
        assertFalse(record.toString().contains("opaque"))
    }

    @Test
    fun `invalid lifetime overflow wrong boot and already expired responses fail closed`() {
        for ((issued, expires) in listOf(-1L to 300L, 1L to 1L, 1L to 60L, 1L to 902L, Long.MAX_VALUE to 0L)) {
            assertTrue(runCatching { factory.create(scope, "opaque", issued, expires, clock.now, clock.boot) }.isFailure)
        }
        assertTrue(runCatching { factory.create(scope, "opaque", 1, 301, Long.MAX_VALUE, clock.boot) }.isFailure)
        assertTrue(runCatching { factory.create(scope, "opaque", 1, 301, clock.now, clock.boot + 1) }.isFailure)
        clock.now = 400_000
        assertTrue(runCatching { factory.create(scope, "opaque", 1, 301, 1_000, clock.boot) }.isFailure)
    }

    @Test
    fun `response timestamps must be nonnegative JSON integers not coerced strings fractions or overflow`() {
        for (issued in listOf("\"1\"", "1.0", "1e2", "true", "null", "-1", "9223372036854775808")) {
            val json = "{\"access_token\":\"opaque\",\"token_type\":\"Bearer\",\"issued_at\":$issued,\"expires_at\":301}"
            assertTrue(runCatching { gson.fromJson(json, Profile3TokenResponse::class.java) }.isFailure)
        }
        val response = gson.fromJson(
            "{\"access_token\":\"opaque\",\"token_type\":\"Bearer\",\"issued_at\":1,\"expires_at\":301}",
            Profile3TokenResponse::class.java
        )
        assertEquals(1, response.issuedAt)
        assertEquals(301, response.expiresAt)
    }

    @Test
    fun `response rejects missing duplicate and incorrectly typed fields`() {
        val invalid = listOf(
            "{}",
            "{\"access_token\":1,\"token_type\":\"Bearer\",\"issued_at\":1,\"expires_at\":301}",
            "{\"access_token\":\"opaque\",\"token_type\":\"Other\",\"issued_at\":1,\"expires_at\":301}",
            "{\"access_token\":\"opaque\",\"token_type\":\"Bearer\",\"issued_at\":1,\"issued_at\":2,\"expires_at\":301}"
        )
        invalid.forEach { json -> assertTrue(runCatching { gson.fromJson(json, Profile3TokenResponse::class.java) }.isFailure) }
    }

    @Test
    fun `challenge timestamps reject coercion and enforce a sixty second interval`() {
        val challenge = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8"
        for (issued in listOf("\"1\"", "1.0", "true", "9223372036854775808")) {
            val json = "{\"challenge\":\"$challenge\",\"issued_at\":$issued,\"expires_at\":61}"
            assertTrue(runCatching { gson.fromJson(json, Profile3ChallengeResponse::class.java) }.isFailure)
        }
        assertEquals(challenge, requireProfile3Challenge(Profile3ChallengeResponse(challenge, 1, 61)))
        assertTrue(runCatching { requireProfile3Challenge(Profile3ChallengeResponse(challenge, 1, 62)) }.isFailure)
        assertTrue(runCatching { requireProfile3Challenge(Profile3ChallengeResponse(challenge, Long.MAX_VALUE, 1)) }.isFailure)
    }
}
