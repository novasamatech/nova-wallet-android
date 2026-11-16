package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `commission is kept when protected net minimum stays above minimum balance`() {
        val amount = resolver.resolveOrWaive(
            swapLimit = specifiedIn(grossOutput = 1_000, minimumOutput = 105),
            minimumBalance = BigInteger.valueOf(100),
        )

        assertEquals(BigInteger.valueOf(8), amount)
    }

    @Test
    fun `commission is waived when protected net minimum falls below minimum balance`() {
        val amount = resolver.resolveOrWaive(
            swapLimit = specifiedIn(grossOutput = 1_000, minimumOutput = 100),
            minimumBalance = BigInteger.valueOf(100),
        )

        assertNull(amount)
    }

    @Test
    fun `initial quote keeps commission for output below ED`() {
        val limit = specifiedIn(grossOutput = 1_000, minimumOutput = 100)
        val amount = resolver.resolveRequired(limit)

        assertEquals(BigInteger.valueOf(8), amount)
        val protectedLimit = commission.protectMinimumOutput(limit, amount!!) as SwapLimit.SpecifiedIn
        assertEquals(BigInteger.valueOf(99), protectedLimit.amountOutMin - amount)
    }

    @Test
    fun `specified out keeps commission on small output`() {
        val limit = SwapLimit.SpecifiedOut(BigInteger.valueOf(1_000), BigInteger.TEN, BigInteger.TEN)
        assertEquals(BigInteger.valueOf(8), resolver.resolveRequired(limit))
    }

    @Test
    fun `zero and rounded down commission do not add a transfer`() {
        assertNull(resolver.resolveRequired(specifiedIn(grossOutput = 0, minimumOutput = 0)))
        assertNull(resolver.resolveRequired(specifiedIn(grossOutput = 118, minimumOutput = 118)))
        assertEquals(BigInteger.ONE, resolver.resolveRequired(specifiedIn(grossOutput = 119, minimumOutput = 119)))
    }

    private fun specifiedIn(grossOutput: Long, minimumOutput: Long): SwapLimit.SpecifiedIn {
        return SwapLimit.SpecifiedIn(
            amountIn = BigInteger.valueOf(1_000),
            amountOutQuote = BigInteger.valueOf(grossOutput),
            amountOutMin = BigInteger.valueOf(minimumOutput),
        )
    }
}
