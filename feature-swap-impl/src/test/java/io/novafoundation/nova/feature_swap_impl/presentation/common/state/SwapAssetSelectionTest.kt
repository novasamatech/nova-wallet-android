package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import io.novafoundation.nova.common.utils.Precision
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SwapAssetSelectionTest {
    private val pay = asset(10)
    private val receive = asset(6)
    private val chosen = asset(12)

    @Test
    fun `incompatible pay choice clears receive and amount in one emission`() = runTest {
        val state = RealSwapSettingsState(SwapSettings(pay, receive, BigInteger.TEN, SwapDirection.SPECIFIED_OUT, DEFAULT_SLIPPAGE))
        val values = mutableListOf<SwapSettings>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.selectedOption.collect(values::add) }
        state.applyAssetSelection(chosen, selectingPay = true, clearOpposite = true)
        assertEquals(2, values.size)
        assertEquals(chosen, values.last().assetIn)
        assertNull(values.last().assetOut)
        assertNull(values.last().amount)
        assertEquals(DEFAULT_SLIPPAGE, values.last().slippage)
        collection.cancel()
    }

    @Test
    fun `incompatible receive choice clears pay`() {
        val state = RealSwapSettingsState(SwapSettings(pay, receive, BigInteger.TEN, SwapDirection.SPECIFIED_IN, DEFAULT_SLIPPAGE))
        state.applyAssetSelection(chosen, selectingPay = false, clearOpposite = true)
        assertNull(state.selectedOption.value.assetIn)
        assertEquals(chosen, state.selectedOption.value.assetOut)
        assertNull(state.selectedOption.value.amount)
    }

    @Test
    fun `compatible replacement preserves decimal amount across precisions`() {
        val state = RealSwapSettingsState(SwapSettings(pay, receive, BigInteger.TEN.pow(10), SwapDirection.SPECIFIED_IN, DEFAULT_SLIPPAGE))
        state.applyAssetSelection(chosen, selectingPay = true, clearOpposite = false)
        assertEquals(BigInteger.TEN.pow(12), state.selectedOption.value.amount)
        assertEquals(receive, state.selectedOption.value.assetOut)
    }

    @Test
    fun `choosing receive does not reinterpret a pay amount`() {
        val state = RealSwapSettingsState(SwapSettings(pay, null, BigInteger.TEN, SwapDirection.SPECIFIED_IN, DEFAULT_SLIPPAGE))
        state.applyAssetSelection(chosen, selectingPay = false, clearOpposite = false)
        assertEquals(BigInteger.TEN, state.selectedOption.value.amount)
        assertEquals(pay, state.selectedOption.value.assetIn)
    }

    private fun asset(precision: Int): Chain.Asset = mock(Chain.Asset::class.java).also { `when`(it.precision).thenReturn(Precision(precision)) }
}
