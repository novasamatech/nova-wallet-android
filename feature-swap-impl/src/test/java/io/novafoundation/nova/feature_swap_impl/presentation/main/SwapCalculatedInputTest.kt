package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.RealSwapSettingsState
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger

class SwapCalculatedInputTest {

    @Test
    fun `clearing the specified amount clears the previous quote in either direction`() {
        SwapDirection.entries.forEach { direction ->
            val form = QuotedForm(direction)
            form.specifiedInput.inputState.value = InputState("", initiatedByUser = true, inputKind = InputKind.REGULAR)

            form.changeSettings { setAmount(null, direction) }

            assertEquals(InputState("", initiatedByUser = true, inputKind = InputKind.REGULAR), form.specifiedInput.inputState.value)
            form.assertCalculatedInputCleared()
        }
    }

    @Test
    fun `changing the specified amount clears the previous quote while retaining the edit`() {
        SwapDirection.entries.forEach { direction ->
            val form = QuotedForm(direction)
            val editedInput = InputState("20", initiatedByUser = true, inputKind = InputKind.REGULAR)
            form.specifiedInput.inputState.value = editedInput

            form.changeSettings { setAmount(BigInteger.valueOf(20), direction) }

            assertEquals(editedInput, form.specifiedInput.inputState.value)
            form.assertCalculatedInputCleared()
        }
    }

    @Test
    fun `selecting a new counterpart clears the previous quote while retaining the specified amount`() {
        SwapDirection.entries.forEach { direction ->
            val form = QuotedForm(direction)
            val selectedAsset = mock(Chain.Asset::class.java)
            val specifiedInput = form.specifiedInput.inputState.value

            form.changeSettings {
                applyAssetSelection(selectedAsset, selectingPay = direction == SwapDirection.SPECIFIED_OUT, clearOpposite = false)
            }

            assertEquals(specifiedInput, form.specifiedInput.inputState.value)
            form.assertCalculatedInputCleared()
        }
    }

    @Test
    fun `refresh and slippage change retain the calculated amount`() {
        val form = QuotedForm(SwapDirection.SPECIFIED_IN)
        val calculatedInput = form.calculatedInput.inputState.value

        form.changeSettings { }
        assertEquals(calculatedInput, form.calculatedInput.inputState.value)

        form.changeSettings { setSlippage(1.percents) }
        assertEquals(calculatedInput, form.calculatedInput.inputState.value)
    }

    private class QuotedForm(direction: SwapDirection) {

        private val assetIn = mock(Chain.Asset::class.java)

        private val assetOut = mock(Chain.Asset::class.java)

        private val state = RealSwapSettingsState(SwapSettings(assetIn, assetOut, BigInteger.TEN, direction, DEFAULT_SLIPPAGE))

        val specifiedInput = input("10", initiatedByUser = true)

        val calculatedInput = input("15", initiatedByUser = false)

        private val inputIn = if (direction == SwapDirection.SPECIFIED_IN) specifiedInput else calculatedInput

        private val inputOut = if (direction == SwapDirection.SPECIFIED_OUT) specifiedInput else calculatedInput

        fun changeSettings(update: RealSwapSettingsState.() -> Unit) {
            val previousSettings = state.selectedOption.value
            state.update()
            state.selectedOption.value.invalidateCalculatedInput(previousSettings, inputIn, inputOut)
        }

        fun assertCalculatedInputCleared() {
            assertEquals(InputState("", initiatedByUser = false, inputKind = InputKind.REGULAR), calculatedInput.inputState.value)
        }

        private fun input(amount: String, initiatedByUser: Boolean): SwapAmountInputMixin {
            val inputState = MutableStateFlow(InputState(amount, initiatedByUser, InputKind.REGULAR))
            val mixin = mock(SwapAmountInputMixin::class.java)
            `when`(mixin.inputState).thenReturn(inputState)
            return mixin
        }
    }
}
