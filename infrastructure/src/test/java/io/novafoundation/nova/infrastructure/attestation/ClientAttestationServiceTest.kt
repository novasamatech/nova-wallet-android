package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.common.utils.IntegrityService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

private const val APP_PACKAGE = "io.novafoundation.nova.test"

private class RecordingKeyPairStore : AttestationKeyPairStore {

    val deleted = mutableListOf<String>()

    override fun publicKey(alias: String) = alias.toByteArray()

    override fun sign(alias: String, payload: ByteArray) = payload

    override fun delete(alias: String) {
        deleted += alias
    }
}

private class UnusedAttestationApi : AttestationApi {

    override suspend fun challenge() = error("Registration is not exercised here")

    override suspend fun register(request: AttestationRegisterRequest) = error("Registration is not exercised here")
}

class ClientAttestationServiceTest {

    private lateinit var identity: RealAttestationIdentity
    private lateinit var keyPairStore: RecordingKeyPairStore

    @Before
    fun setUp() {
        identity = RealAttestationIdentity(InMemoryPreferences())
        keyPairStore = RecordingKeyPairStore()
    }

    private fun service(mode: AttestationMode = AttestationMode.SHARED_SECRET) = RealClientAttestationService(
        api = UnusedAttestationApi(),
        identity = identity,
        keyPairStore = keyPairStore,
        integrityService = mock(IntegrityService::class.java),
        appPackage = APP_PACKAGE,
        mode = mode,
        sharedSecret = "test-secret"
    )

    @Test
    fun `a rejected client is rebuilt under a new identity`() = runBlocking<Unit> {
        val rejected = identity.clientId()
        identity.markAttested()

        service().invalidate(rejected)

        // Re-registering under the same id would be refused, so recovery means a new one
        assertNotEquals(rejected, identity.clientId())
        assertFalse(identity.isAttested())
        assertEquals(listOf(rejected), keyPairStore.deleted)
    }

    @Test
    fun `a rejection naming an identity we already replaced changes nothing`() = runBlocking<Unit> {
        val current = identity.clientId()
        identity.markAttested()

        service().invalidate("11111111-2222-4333-8444-555555555555")

        // Otherwise a burst of rejected in-flight requests would each throw away a registration
        // that had just been made, and the client would never settle.
        assertEquals(current, identity.clientId())
        assertTrue(identity.isAttested())
        assertTrue(keyPairStore.deleted.isEmpty())
    }

    @Test
    fun `a build without attestation has no identity to drop`() = runBlocking<Unit> {
        val current = identity.clientId()

        service(mode = AttestationMode.UNATTESTED).invalidate(current)

        assertEquals(current, identity.clientId())
        assertTrue(keyPairStore.deleted.isEmpty())
    }
}
