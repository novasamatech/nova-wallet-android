package io.novafoundation.nova.common.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock

class DebugSettingsTest {

    private val values = mutableMapOf<String, MutableStateFlow<Boolean>>()

    private val preferences = mock(Preferences::class.java) { invocation ->
        when (invocation.method.name) {
            "getBoolean" -> values.getOrPut(invocation.getArgument(0)) { MutableStateFlow(false) }.value
            "booleanFlow" -> values.getOrPut(invocation.getArgument(0)) { MutableStateFlow(false) }
            "putBoolean" -> {
                values.getOrPut(invocation.getArgument(0)) { MutableStateFlow(false) }.value = invocation.getArgument(1)
                null
            }
            else -> RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val settings = DebugSettings(preferences)

    @Test
    fun `node logging toggle is persistent reversible and independent from claim`() {
        assertFalse(settings.nodeLogsDisabled())
        settings.setNodeLogsDisabled(true)
        val reopened = DebugSettings(preferences)
        assertTrue(reopened.nodeLogsDisabled())
        assertFalse(reopened.bittensorClaimEnabled())
        reopened.setNodeLogsDisabled(false)
        assertFalse(settings.nodeLogsDisabled())
    }

    @Test
    fun `button can be enabled without dry run`() = runBlocking {
        settings.setBittensorClaimButtonEnabled(true)
        assertTrue(settings.bittensorClaimButtonEnabled())
        assertTrue(settings.observeBittensorClaimButton().first())
        assertFalse(settings.bittensorClaimEnabled())
    }

    @Test
    fun `dry run enables button and disabling button disables dry run`() = runBlocking {
        settings.setBittensorClaimEnabled(true)
        assertTrue(settings.bittensorClaimButtonEnabled())
        settings.setBittensorClaimEnabled(false)
        assertTrue(settings.bittensorClaimButtonEnabled())
        assertFalse(settings.bittensorClaimEnabled())
        settings.setBittensorClaimEnabled(true)
        settings.setBittensorClaimButtonEnabled(false)
        assertFalse(settings.bittensorClaimEnabled())
        assertFalse(settings.observeBittensorClaimButton().first())
    }

    @Test
    fun `claim debug is opt in and disabling is observed`() = runBlocking {
        assertFalse(settings.bittensorClaimEnabled())
        assertFalse(settings.observeBittensorClaim().first())
        settings.setBittensorClaimEnabled(true)
        assertTrue(settings.bittensorClaimEnabled())
        assertTrue(settings.observeBittensorClaim().first())
        settings.setBittensorClaimEnabled(false)
        assertFalse(settings.bittensorClaimEnabled())
        assertFalse(settings.observeBittensorClaim().first())
    }
}
