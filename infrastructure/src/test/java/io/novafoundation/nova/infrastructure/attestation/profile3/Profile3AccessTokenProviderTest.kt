package io.novafoundation.nova.infrastructure.attestation.profile3

import io.novafoundation.nova.infrastructure.attestation.AttestationRefusedException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnavailableException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnsupportedException
import io.novafoundation.nova.infrastructure.attestation.isAttestationUnsupported
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Profile3AccessTokenProviderTest {

    @Test
    fun `registers and issues profile3 once then reuses persisted bearer after restart`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val record = provider.token(fixture.configuredOrigin)
        assertEquals(1, fixture.api.registrationRequests.size)
        assertEquals(1, fixture.api.tokenRequests.size)
        assertTrue(fixture.api.challenges.all { it.profile == 3 })
        assertTrue(fixture.api.registrationRequests.all { it.profile == 3 })
        assertTrue(fixture.api.tokenRequests.all { it.profile == 3 })
        assertTrue(fixture.api.destinations.all { "/v1/attestation/profile-3/" in it })
        assertSame(record, provider.token(fixture.configuredOrigin))
        val restarted = fixture.provider()
        assertSame(record, restarted.token(fixture.configuredOrigin))
        assertEquals(1, fixture.api.tokenRequests.size)
    }

    @Test
    fun `parallel requests share issuance and one cancelled waiter does not cancel the owner`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val gate = CompletableDeferred<Unit>()
        fixture.api.beforeToken = { gate.await() }
        val provider = fixture.provider()
        val first = async { provider.token(fixture.configuredOrigin) }
        val second = async { provider.token(fixture.configuredOrigin) }
        runCurrent()
        first.cancel()
        gate.complete(Unit)
        val record = second.await()
        assertEquals(1, fixture.api.tokenRequests.size)
        assertEquals(record.accessToken, fixture.store.records[record.scope]?.accessToken)
    }

    @Test
    fun `late rejection cannot invalidate the replacement token`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val old = provider.token(fixture.configuredOrigin)
        provider.invalidate(old)
        val replacement = provider.token(fixture.configuredOrigin)
        provider.invalidate(old)
        assertSame(replacement, provider.token(fixture.configuredOrigin))
        assertEquals(replacement.accessToken, fixture.store.records[replacement.scope]?.accessToken)
        assertEquals(2, fixture.api.tokenRequests.size)
    }

    @Test
    fun `early refresh failure keeps the usable bearer and observes backoff`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val current = provider.token(fixture.configuredOrigin)
        fixture.clock.now = current.refreshAtElapsedRealtimeMillis
        fixture.api.failure = IOException("offline")
        assertSame(current, provider.token(fixture.configuredOrigin))
        runCurrent()
        assertSame(current, provider.token(fixture.configuredOrigin))
        assertEquals(2, fixture.api.tokenRequests.size)
    }

    @Test
    fun `bootstrap 429 does not loop or discard an existing usable token`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val current = provider.token(fixture.configuredOrigin)
        fixture.clock.now = current.refreshAtElapsedRealtimeMillis
        fixture.api.failure = httpFailure(429, "rate_limited", "120")
        assertSame(current, provider.token(fixture.configuredOrigin))
        runCurrent()
        fixture.clock.now += 60_000
        assertTrue(runCatching { provider.token(fixture.configuredOrigin) }.isFailure)
        assertEquals(2, fixture.api.tokenRequests.size)
    }

    @Test
    fun `business rate limit defers network without minting or invalidating a token`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val current = provider.token(fixture.configuredOrigin)
        provider.recordResponse(current, 429, "rate_limited", "30")
        assertSame(current, provider.token(fixture.configuredOrigin))
        assertTrue(runCatching { provider.beforeNetwork(fixture.configuredOrigin) }.isFailure)
        fixture.clock.now += 30_000
        provider.beforeNetwork(fixture.configuredOrigin)
        assertEquals(1, fixture.api.tokenRequests.size)
    }

    @Test
    fun `missing keystore key replaces identity but token expiry does not`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val old = provider.token(fixture.configuredOrigin)
        fixture.clock.now = old.deadlineElapsedRealtimeMillis
        val renewed = provider.token(fixture.configuredOrigin)
        assertEquals(old.scope, renewed.scope)
        fixture.keys.aliases.clear()
        val recovered = provider.token(fixture.configuredOrigin)
        assertNotEquals(old.scope, recovered.scope)
        assertFalse(old.scope in fixture.store.records)
    }

    @Test
    fun `unknown client re-registers same identity and uses a fresh challenge`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val old = provider.token(fixture.configuredOrigin)
        provider.invalidate(old)
        fixture.api.knownClient = false
        val recovered = provider.token(fixture.configuredOrigin)
        assertEquals(old.scope, recovered.scope)
        assertEquals(2, fixture.api.registrationRequests.size)
    }

    @Test
    fun `failed persistence prevents the first use of an issued bearer`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        fixture.store.failWrite = true
        val provider = fixture.provider()
        assertTrue(runCatching { provider.token(fixture.configuredOrigin) }.isFailure)
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test
    fun `origin change removes only old profile3 identity and rejects foreign destinations`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val provider = fixture.provider()
        val old = provider.token(fixture.configuredOrigin)
        fixture.configuredOrigin = "https://other.test/".toHttpUrl()
        assertTrue(runCatching { provider.token("https://infra.test/".toHttpUrl()) }.isFailure)
        val new = provider.token(fixture.configuredOrigin)
        assertNotEquals(old.scope, new.scope)
        assertFalse(old.scope in fixture.store.records)
        assertEquals(1, fixture.keys.aliases.size)
    }

    @Test
    fun `registration accepts only committed empty 204`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        fixture.api.registerStatus = 200
        val provider = fixture.provider()
        assertTrue(runCatching { provider.token(fixture.configuredOrigin) }.isFailure)
        assertTrue(fixture.api.tokenRequests.isEmpty())
        assertFalse(fixture.identities.current()!!.registered)
    }

    @Test
    fun `negative platform verdict stops automatic issuance without erasing identity`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        fixture.api.failure = httpFailure(401, "attestation_failed")
        val provider = fixture.provider()
        assertTrue(runCatching { provider.token(fixture.configuredOrigin) }.isFailure)
        val identity = fixture.identities.current()!!
        fixture.clock.now += 60_000
        assertTrue(runCatching { provider.token(fixture.configuredOrigin) }.isFailure)
        assertEquals(1, fixture.api.tokenRequests.size)
        assertEquals(identity.scope, fixture.identities.current()?.scope)
    }

    @Test
    fun `backend refusal keeps its status and code on every later request`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        fixture.api.failure = httpFailure(401, "attestation_failed")
        val provider = fixture.provider()
        val first = runCatching { provider.token(fixture.configuredOrigin) }.exceptionOrNull()
        val refused = runCatching { provider.token(fixture.configuredOrigin) }.exceptionOrNull()
        listOf(first, refused).forEach { failure ->
            assertTrue(failure is AttestationRefusedException)
            assertEquals(401, (failure as AttestationRefusedException).status)
            assertEquals("attestation_failed", failure.code)
            assertFalse(failure.isAttestationUnsupported())
        }
    }

    @Test
    fun `device without play integrity is unsupported even while cooling down and recovers once attested`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        fixture.integrityFailure = AttestationUnsupportedException("Play Integrity is unavailable on this device")
        val provider = fixture.provider()
        val first = runCatching { provider.token(fixture.configuredOrigin) }.exceptionOrNull()
        assertTrue(first is AttestationUnsupportedException)
        val cooling = runCatching { provider.token(fixture.configuredOrigin) }.exceptionOrNull()
        assertTrue(cooling is AttestationUnsupportedException)
        assertTrue(fixture.api.registrationRequests.isEmpty())

        fixture.integrityFailure = null
        fixture.clock.now += 60_000
        provider.token(fixture.configuredOrigin)
        assertEquals(1, fixture.api.registrationRequests.size)
    }

    @Test
    fun `transient bootstrap failure stays retryable`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        fixture.api.failure = IOException("offline")
        val provider = fixture.provider()
        val failure = runCatching { provider.token(fixture.configuredOrigin) }.exceptionOrNull()
        assertTrue(failure is AttestationUnavailableException)
        assertFalse(failure!!.isAttestationUnsupported())
        assertFalse(runCatching { provider.token(fixture.configuredOrigin) }.exceptionOrNull()!!.isAttestationUnsupported())
    }

    @Test
    fun `cancelled old origin flight cannot publish a late noncooperative result`() = runTest {
        val fixture = Profile3Fixture(backgroundScope)
        val release = CompletableDeferred<Unit>()
        fixture.api.beforeToken = { withContext(NonCancellable) { release.await() } }
        val provider = fixture.provider()
        val oldFlight = async { runCatching { provider.token(fixture.configuredOrigin) } }
        runCurrent()
        val oldScope = fixture.identities.current()!!.scope
        fixture.api.beforeToken = {}
        fixture.configuredOrigin = "https://other.test/".toHttpUrl()
        val replacement = provider.token(fixture.configuredOrigin)
        release.complete(Unit)
        assertTrue(oldFlight.await().isFailure)
        runCurrent()
        assertFalse(oldScope in fixture.store.records)
        assertEquals(setOf(replacement.scope), fixture.store.records.keys)
        assertSame(replacement, provider.token(fixture.configuredOrigin))
    }
}
