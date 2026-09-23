package io.novafoundation.nova.feature_swap_impl.domain.model

import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

class NovaSwapCommissionTest {

    private val commission = NovaSwapCommission()

    @Test
    fun `commission is added on top of net amount`() {
        assertEquals(BigInteger.valueOf(85), commission.commissionToAddOnTop(BigInteger.valueOf(10_000)))
    }

    @Test
    fun `commission is extracted from gross amount`() {
        assertEquals(BigInteger.valueOf(85), commission.commissionIncludedIn(BigInteger.valueOf(10_085)))
    }

    @Test
    fun `commission is extracted from current swap limit`() {
        val swapLimit = SwapLimit.SpecifiedIn(
            amountIn = BigInteger.valueOf(20_000),
            amountOutQuote = BigInteger.valueOf(10_085),
            amountOutMin = BigInteger.valueOf(10_000),
        )

        assertEquals(BigInteger.valueOf(85), commission.commissionIncludedIn(swapLimit))
    }

    @Test
    fun `protected minimum guarantees displayed minimum after fixed commission`() {
        val swapLimit = SwapLimit.SpecifiedIn(
            amountIn = BigInteger.valueOf(20_000),
            amountOutQuote = BigInteger.valueOf(10_085),
            amountOutMin = BigInteger.valueOf(10_000),
        )

        val protected = commission.protectMinimumOutput(swapLimit, BigInteger.valueOf(85)) as SwapLimit.SpecifiedIn
        val displayedMinimum = commission.minimumAmountOutAfterCommission(swapLimit, BigInteger.valueOf(85))

        assertEquals(displayedMinimum + BigInteger.valueOf(85), protected.amountOutMin)
        assertEquals(displayedMinimum, protected.amountOutMin - BigInteger.valueOf(85))
    }

    @Test
    fun `specified out limit is unchanged by minimum protection`() {
        val swapLimit = SwapLimit.SpecifiedOut(
            amountOut = BigInteger.valueOf(10_085),
            amountInQuote = BigInteger.valueOf(20_000),
            amountInMax = BigInteger.valueOf(21_000),
        )

        assertEquals(swapLimit, commission.protectMinimumOutput(swapLimit, BigInteger.valueOf(85)))
    }

    @Test
    fun `asset hub beneficiary is configured only for polkadot asset hub`() {
        assertArrayEquals(
            NovaSwapCommission.POLKADOT_ASSET_HUB_FEE_ACCOUNT_HEX.hexToBytes(),
            commission.assetHubFeeAccountId(Chain.Geneses.POLKADOT_ASSET_HUB)
        )
        assertNull(commission.assetHubFeeAccountId("unsupported"))
    }

    private fun String.hexToBytes(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
