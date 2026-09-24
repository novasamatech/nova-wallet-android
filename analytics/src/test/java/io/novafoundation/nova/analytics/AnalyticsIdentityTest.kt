package io.novafoundation.nova.analytics

import io.novafoundation.nova.analytics.transport.RealAnalyticsIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class AnalyticsIdentityTest {

    private lateinit var preferences: InMemoryPreferences
    private lateinit var identity: RealAnalyticsIdentity

    @Before
    fun setUp() {
        preferences = InMemoryPreferences()
        identity = RealAnalyticsIdentity(preferences)
    }

    @Test
    fun `install id is stable across calls and instances`() {
        val first = identity.installId()

        assertEquals(first, identity.installId())
        assertEquals(first, RealAnalyticsIdentity(preferences).installId())
    }

    @Test
    fun `session id changes between instances`() {
        assertNotEquals(identity.sessionId, RealAnalyticsIdentity(preferences).sessionId)
    }

    @Test
    fun `withdrawing consent rotates install id`() {
        val installId = identity.installId()

        identity.resetInstallId()

        assertNotEquals(installId, identity.installId())
    }
}
