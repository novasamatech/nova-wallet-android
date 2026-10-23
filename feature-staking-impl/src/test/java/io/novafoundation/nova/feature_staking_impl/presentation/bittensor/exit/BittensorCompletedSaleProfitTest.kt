package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedPurchaseTotals
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionWithdrawal
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.hasWithdrawalRestriction
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorCompletedSaleProfitTest {

    private val netuid = BittensorNetuid.Subnet(1)

    // Bought 100 alpha for 50 TAO units: the cost of 10 alpha is 5.
    private val totals = BittensorEstimatedPurchaseTotals(BigInteger.valueOf(50), BigInteger.valueOf(100))

    private val quote = BittensorExitQuote(
        netuid = netuid,
        amount = BigInteger.TEN,
        fullExit = false,
        estimatedTaoOut = BigInteger.valueOf(8),
        minimumTaoOut = BigInteger.valueOf(7),
        serviceFee = BigInteger.ONE,
        limitPrice = BigInteger.ONE
    )

    private fun outcome(tao: BigInteger?, alpha: BigInteger?, serviceFee: BigInteger?) = BittensorOperationOutcome(
        blockHash = "0x",
        tao = tao,
        alpha = alpha,
        serviceFee = serviceFee,
        networkFee = BigInteger.ONE,
        feePayer = null,
        alphaFee = null,
        netuid = netuid,
        coldkey = AccountIdKey(ByteArray(32))
    )

    @Test
    fun `executed amounts replace the quote`() {
        // 9 TAO out - 1 service - cost of 10 alpha (5) = 3
        assertEquals(BigInteger.valueOf(3), completedSaleProfit(totals, quote, outcome(BigInteger.valueOf(9), BigInteger.TEN, BigInteger.ONE)))
        // A loss is negative: 5 - 1 - 5
        assertEquals(BigInteger.valueOf(-1), completedSaleProfit(totals, quote, outcome(BigInteger.valueOf(5), BigInteger.TEN, BigInteger.ONE)))
    }

    @Test
    fun `missing executed amounts fall back to the reviewed quote`() {
        val estimate = BigInteger.valueOf(2) // 8 - 1 - 5
        assertEquals(estimate, completedSaleProfit(totals, quote, null))
        assertEquals(estimate, completedSaleProfit(totals, quote, outcome(null, BigInteger.TEN, BigInteger.ONE)))
        assertEquals(estimate, completedSaleProfit(totals, quote, outcome(BigInteger.valueOf(9), BigInteger.TEN, null)))
    }

    @Test
    fun `a sale without a service fee does not need the paid service fee`() {
        val free = quote.copy(serviceFee = BigInteger.ZERO)
        assertEquals(BigInteger.valueOf(4), completedSaleProfit(totals, free, outcome(BigInteger.valueOf(9), BigInteger.TEN, null)))
    }

    @Test
    fun `withdrawal caption is only for locked or held stake`() {
        val free = BittensorPositionWithdrawal(BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO, canWithdraw = true)
        assertFalse(free.hasWithdrawalRestriction())
        assertTrue(free.copy(locked = BigInteger.ONE).hasWithdrawalRestriction())
        assertTrue(free.copy(holdDurationMillis = BigInteger.ONE).hasWithdrawalRestriction())
    }
}
