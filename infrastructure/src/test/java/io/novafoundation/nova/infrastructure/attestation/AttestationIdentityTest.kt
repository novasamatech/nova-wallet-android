package io.novafoundation.nova.infrastructure.attestation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttestationIdentityTest {

    private lateinit var preferences: InMemoryPreferences
    private lateinit var identity: RealAttestationIdentity

    @Before
    fun setUp() {
        preferences = InMemoryPreferences()
        identity = RealAttestationIdentity(preferences)
    }

    @Test
    fun `client id is stable across calls and instances`() {
        val first = identity.clientId()

        assertEquals(first, identity.clientId())
        assertEquals(first, RealAttestationIdentity(preferences).clientId())
    }

    @Test
    fun `attestation flag is not set until marked`() {
        assertFalse(identity.isAttested())

        identity.markAttested()

        assertTrue(identity.isAttested())
        assertTrue(RealAttestationIdentity(preferences).isAttested())
    }
}
