package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class AssetConversionCommissionTest {

    private val commission = NovaSwapCommission()
    private val resolver = AssetHubCommissionResolver(commission)

    @Test
    fun `commission is recalculated from updated output`() {
        val amount = resolver.resolveOrWaive(
            swapLimit = specifiedIn(grossOutput = 5_042, minimumOutput = 5_000),
            minimumBalance = BigInteger.ONE,
        )

        assertEquals(BigInteger.valueOf(42), amount)
    }

    @Test
    fun `commission is waived when updated output would fall below minimum balance`() {
        val amount = resolver.resolveOrWaive(
            swapLimit = specifiedIn(grossOutput = 1_000, minimumOutput = 105),
            minimumBalance = BigInteger.valueOf(100),
        )

        assertNull(amount)
    }

    @Test
    fun `initial quote rejects commission below minimum balance`() {
        val result = runCatching {
            resolver.resolveRequired(
                swapLimit = specifiedIn(grossOutput = 1_000, minimumOutput = 105),
                minimumBalance = BigInteger.valueOf(100),
            )
        }
        assertTrue(result.isFailure)
    }

    private fun specifiedIn(grossOutput: Long, minimumOutput: Long): SwapLimit.SpecifiedIn {
        return SwapLimit.SpecifiedIn(
            amountIn = BigInteger.valueOf(1_000),
            amountOutQuote = BigInteger.valueOf(grossOutput),
            amountOutMin = BigInteger.valueOf(minimumOutput),
        )
    }
}
