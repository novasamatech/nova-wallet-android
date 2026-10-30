package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorExitAmountInputTest {

    @Test
    fun `max follows availability changes including zero`() {
        val selected = ExitAmountInput("").selectMax(BigDecimal("1.234567891"))
        assertEquals("1.234567891", selected.text)
        assertEquals("0.5", selected.updateMax(BigDecimal("0.5")).text)
        assertEquals("0", selected.updateMax(BigDecimal.ZERO).text)
        assertEquals("0", selected.updateMax(BigDecimal("-1")).text)
    }

    @Test
    fun `programmatic text echo and quote refresh preserve max mode`() {
        val selected = ExitAmountInput("").selectMax(BigDecimal.ONE)
        val refreshed = selected.edit("1").copy(revision = 1)
        val updated = refreshed.updateMax(BigDecimal.TEN)
        assertEquals("10", updated.text)
        assertEquals(1L, updated.revision)
        assertEquals(InputKind.MAX_ACTION, updated.inputKind)
    }

    @Test
    fun `manual edit or clearing stops max updates until max selected again`() {
        val selected = ExitAmountInput("").selectMax(BigDecimal.ONE)
        for (text in listOf("0.2", "")) {
            val edited = selected.edit(text)
            assertEquals(text, edited.updateMax(BigDecimal.TEN).text)
            assertEquals(InputKind.REGULAR, edited.inputKind)
            assertEquals("2", edited.selectMax(BigDecimal.ONE).updateMax(BigDecimal("2")).text)
        }
    }

    @Test
    fun `unavailable maximum does not erase input or max selection`() {
        val selected = ExitAmountInput("").selectMax(BigDecimal.ONE)
        assertEquals(selected, selected.updateMax(null))
        assertEquals("2", selected.updateMax(null).updateMax(BigDecimal("2")).text)
        assertEquals("", ExitAmountInput("").updateMax(BigDecimal.TEN).text)
    }
}
