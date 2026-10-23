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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorExitConsentCarryTest {

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
    fun `setup proceed carries to the unchanged reviewed quote`() {
        val payload = quote.toPayload(position).copy(remainderSweepAccepted = true)

        assertTrue(payload.carriesRemainderSweepFor(payload.quote()))
        assertTrue(payload.carriesRemainderSweepFor(quote))
    }

    @Test
    fun `refreshed quote or max asks again`() {
        val payload = quote.toPayload(position).copy(remainderSweepAccepted = true)

        assertFalse(payload.carriesRemainderSweepFor(quote.copy(estimatedTaoOut = BigInteger.valueOf(6_900_000))))
        assertFalse(payload.carriesRemainderSweepFor(quote.copy(fullExit = true, exitHotkeys = listOf(hotkey))))
    }

    @Test
    fun `no consent is carried when the setup did not proceed past the warning`() {
        assertFalse(quote.toPayload(position).carriesRemainderSweepFor(quote))
    }

    @Test
    fun `setup cost basis survives the payload round trip`() {
        val totals = BittensorEstimatedPurchaseTotals(BigInteger.valueOf(20_000_000), BigInteger.valueOf(298_000_000))
        val payload = quote.toPayload(position).copy(costBasis = totals.toPayload())

        assertEquals(totals, payload.costBasis?.totals())
    }
}
