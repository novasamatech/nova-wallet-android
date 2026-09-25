package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class BittensorSubnetQuoteInteractorTest {

    private val subnet = BittensorSubnetId(64)
    private val commission = BittensorSubnetCommission(BigDecimal("0.003"))
    private var partial = false
    private var cancellation = false
    private var requestedInput: BigInteger? = null
    private val source = object : BittensorSwapSource {
        override suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorSubnetId): BigInteger = BigInteger.valueOf(1000)

        override suspend fun simulate(
            chainId: String,
            subnetId: BittensorSubnetId,
            direction: BittensorSwapDirection,
            amountIn: BigInteger
        ): BittensorSwapSimulation {
            if (cancellation) throw CancellationException("Quote replaced")
            requestedInput = amountIn
            val runtimeFee = BigInteger.TEN
            return BittensorSwapSimulation(
                subnetId, direction, amountIn - runtimeFee - if (partial) BigInteger.ONE else BigInteger.ZERO,
                BigInteger.valueOf(1234), runtimeFee, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
            )
        }
    }

    @Test
    fun `Nova fee is included in total input and simulation receives only the stake amount`() = runBlocking {
        val quote = interactor().quoteBuy("bittensor", subnet, BigInteger.valueOf(1_000_000_000), slippage())
        assertEquals(BigInteger.valueOf(3_000_000), quote.serviceFeeTaoInPlanks)
        assertEquals(BigInteger.valueOf(997_000_000), requestedInput)
        assertEquals(requestedInput, quote.simulation.amountIn + quote.simulation.taoFee)
        assertEquals(quote.totalTaoInPlanks, quote.stakeTaoInPlanks + quote.serviceFeeTaoInPlanks)
        assertEquals(BigInteger.valueOf(1234), quote.alphaOutInPlanks) // Runtime swap fees must not be subtracted twice.
        assertEquals(BigInteger.valueOf(1005), quote.limitPrice)
    }

    @Test
    fun `tiny commission rounds down rather than charging a whole rao`() {
        assertEquals(BigInteger.ZERO, commission.feeFor(BigInteger.ONE))
        assertEquals(BigInteger.ONE, commission.feeFor(BigInteger.valueOf(334)))
    }

    @Test
    fun `partial simulation cannot be used with allow partial false`() = runBlocking {
        partial = true
        assertTrue(runCatching { interactor().quoteBuy("bittensor", subnet, BigInteger.valueOf(1000), slippage()) }.isFailure)
    }

    @Test
    fun `cancellation is propagated`() = runBlocking {
        cancellation = true
        val result = runCatching { interactor().quoteBuy("bittensor", subnet, BigInteger.valueOf(1000), slippage()) }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `invalid rates and inputs fail closed`() {
        for (rate in listOf("-0.003", "1", "2")) {
            assertTrue(runCatching { BittensorSubnetCommission(BigDecimal(rate)) }.isFailure)
        }
        for (input in listOf(BigInteger.ZERO, -BigInteger.ONE, BigInteger.ONE.shiftLeft(64))) {
            assertTrue(runCatching { commission.feeFor(input) }.isFailure)
        }
    }

    private fun interactor() = BittensorSubnetQuoteInteractor(source, commission)
    private fun slippage() = BittensorSlippage(BigDecimal("0.005"))
}
