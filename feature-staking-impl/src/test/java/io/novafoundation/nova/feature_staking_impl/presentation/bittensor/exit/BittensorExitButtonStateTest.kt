package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorExitButtonStateTest {

    private fun state(input: String, validating: Boolean = false, loading: Boolean = false, ready: Boolean = true) = bittensorExitButtonState(
        validating = validating,
        input = input,
        loading = loading,
        ready = ready,
        continueLabel = "Continue",
        enterAmountLabel = "Enter amount…"
    )

    @Test
    fun `an empty or zero amount asks for one`() {
        listOf("", "0", "0.000", "abc").forEach { input ->
            val state = state(input, ready = false)
            assertTrue(state is DescriptiveButtonState.Disabled)
            assertEquals("Enter amount…", (state as DescriptiveButtonState.Disabled).reason)
        }
    }

    @Test
    fun `an entered amount continues once the sale is prepared`() {
        val state = state("1.5")
        assertTrue(state is DescriptiveButtonState.Enabled)
        assertEquals("Continue", (state as DescriptiveButtonState.Enabled).action)
    }

    @Test
    fun `an entered amount waits for the quote and stays disabled when it cannot be prepared`() {
        assertEquals(DescriptiveButtonState.Loading, state("1.5", loading = true))
        assertEquals("Continue", (state("1.5", ready = false) as DescriptiveButtonState.Disabled).reason)
    }

    @Test
    fun `validation in progress shows loading even before an amount is parsed`() {
        assertEquals(DescriptiveButtonState.Loading, state("", validating = true))
    }
}
