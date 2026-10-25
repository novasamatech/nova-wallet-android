package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedPurchaseTotals
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPlan
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitSlice
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorExitReviewPayloadTest {

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val quote = BittensorExitQuote(
        netuid = BittensorNetuid.Subnet(64),
        amount = BigInteger.valueOf(100_000_000),
        fullExit = false,
        estimatedTaoOut = BigInteger.valueOf(6_800_000),
        minimumTaoOut = BigInteger.valueOf(6_700_000),
        serviceFee = BigInteger.valueOf(57_000),
        limitPrice = BigInteger.valueOf(67_000_000),
        plan = BittensorExitPlan(listOf(BittensorExitSlice(hotkey, BigInteger.valueOf(100_000_000), false))),
        spotPrice = BigInteger.valueOf(67_300_000),
        simulatedInput = BigInteger.valueOf(99_900_000),
        slippageFraction = BigDecimal("0.005"),
        groupHotkeys = setOf(hotkey),
        serviceFeeRate = BigDecimal("0.0085")
    )

    private val position = BittensorExitPositionPayload(1, 64, "5DU1dv")

    @Test
    fun `reviewed quote survives the payload round trip`() {
        assertEquals(quote, quote.toPayload(position).quote())
    }

    @Test
    fun `setup cost basis survives the payload round trip`() {
        val totals = BittensorEstimatedPurchaseTotals(BigInteger.valueOf(20_000_000), BigInteger.valueOf(298_000_000))
        val payload = quote.toPayload(position).copy(costBasis = totals.toPayload())

        assertEquals(totals, payload.costBasis?.totals())
    }
}
