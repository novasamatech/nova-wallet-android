package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SwapPoolTradeMinimumUiTest {

    private val resourceManager = mock(ResourceManager::class.java)

    private val actions = mock(ValidationFlowActions::class.java)

    private val asset = mock(Chain.Asset::class.java)

    @Test
    fun `WBTC minimum is displayed in eight decimal asset units`() {
        assertMinimumText(1000, 8, "WBTC", "0.00001 WBTC")
    }

    @Test
    fun `intermediate asset minimum uses its own precision and symbol`() {
        assertMinimumText(1000, 12, "HDX", "0.000000001 HDX")
    }

    @Test
    fun `runtime minimum keeps all significant base units`() {
        assertMinimumText(1234, 8, "WBTC", "0.00001234 WBTC")
    }

    @Test
    fun `unavailable pool limits produce a blocking default dialog`() {
        val reason = SwapValidationFailure.PoolTradeLimitsUnavailable(NoSuchElementException())
        `when`(resourceManager.getString(R.string.swap_failure_pool_trade_limits_unavailable_title)).thenReturn("Unavailable")
        `when`(resourceManager.getString(R.string.swap_failure_pool_trade_limits_unavailable_message)).thenReturn("Try again")

        assertTrue(transform(reason) is TransformedFailure.Default)
        verify(resourceManager).getString(R.string.swap_failure_pool_trade_limits_unavailable_message)
    }

    private fun assertMinimumText(minimum: Int, precision: Int, symbol: String, formatted: String) {
        `when`(asset.precision).thenReturn(Precision(precision))
        `when`(asset.symbol).thenReturn(symbol.asTokenSymbol())
        `when`(resourceManager.getString(R.string.swap_failure_pool_trade_minimum_title)).thenReturn("Below minimum")
        `when`(resourceManager.getString(R.string.swap_failure_pool_trade_minimum_message, formatted)).thenReturn(formatted)
        val reason = SwapValidationFailure.AmountBelowPoolTradeMinimum(asset, minimum.toBigInteger())

        assertTrue(transform(reason) is TransformedFailure.Default)
        verify(resourceManager).getString(R.string.swap_failure_pool_trade_minimum_message, formatted)
    }

    private fun transform(reason: SwapValidationFailure): TransformedFailure {
        return mapSwapValidationFailureToUI(
            resourceManager = resourceManager,
            status = reason.validationError(),
            actions = actions,
            amountInSwapMaxAction = { throw AssertionError("Minimum must not offer Swap Max") },
            amountOutSwapMinAction = { _, _ -> throw AssertionError("Must not replace the user amount with an intermediate minimum") },
            amountInSetAction = { _, _ -> throw AssertionError("Must not replace the user amount with an intermediate minimum") },
        )
    }
}
