package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BittensorExitContinuationTest {

    private val slippage = BigDecimal("0.005")

    private fun decision(original: ExitAmountInput, current: ExitAmountInput, requestCurrent: Boolean): ExitContinuationDecision {
        return exitContinuationDecision(original, current, slippage, slippage, requestCurrent)
    }

    @Test
    fun `block or balance update revalidates rather than dropping warning continuation`() {
        val input = ExitAmountInput("1")
        assertEquals(ExitContinuationDecision.REVALIDATE, decision(input, input, requestCurrent = false))
    }

    @Test
    fun `unchanged reviewed request may run warning action`() {
        val input = ExitAmountInput("1")
        assertEquals(ExitContinuationDecision.CONTINUE, decision(input, input, requestCurrent = true))
    }

    @Test
    fun `automatic max after hold release revalidates rather than accepting old impact`() {
        val original = ExitAmountInput("").selectMax(BigDecimal.ONE)
        val updated = original.updateMax(BigDecimal.TEN)
        assertEquals(original.revision, updated.revision)
        assertEquals(ExitContinuationDecision.REVALIDATE, decision(original, updated, requestCurrent = false))
    }

    @Test
    fun `manual edit and edit back invalidate old warning even for same numeric amount`() {
        val original = ExitAmountInput("1")
        val edited = original.edit("2").edit("1")
        assertFalse(original.hasSameIntent(edited))
        assertEquals(ExitContinuationDecision.CANCEL, decision(original, edited, requestCurrent = false))
        assertEquals(ExitContinuationDecision.CANCEL, decision(original, edited, requestCurrent = true))
    }

    @Test
    fun `explicit max selection invalidates old warning but programmatic echo does not`() {
        val original = ExitAmountInput("").selectMax(BigDecimal.ONE)
        val selectedAgain = original.selectMax(BigDecimal.ONE)
        assertEquals(ExitContinuationDecision.CANCEL, decision(original, selectedAgain, requestCurrent = false))
        assertEquals(ExitContinuationDecision.CONTINUE, decision(original, original.edit(original.text), requestCurrent = true))
    }

    @Test
    fun `quote refresh revision or changed slippage cancels obsolete dialog`() {
        val original = ExitAmountInput("1")
        val refreshed = original.copy(revision = original.revision + 1)
        assertEquals(ExitContinuationDecision.CANCEL, decision(original, refreshed, requestCurrent = false))
        assertEquals(
            ExitContinuationDecision.CANCEL,
            exitContinuationDecision(original, original, slippage, BigDecimal("0.01"), requestCurrent = false)
        )
    }
}
