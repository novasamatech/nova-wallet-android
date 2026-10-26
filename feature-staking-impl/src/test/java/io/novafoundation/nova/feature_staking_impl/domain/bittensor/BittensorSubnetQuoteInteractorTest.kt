package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfig
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapQuote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubnetQuoteInteractorTest {

    private val subnet = BittensorNetuid.Subnet(64)

    private val commission = BittensorSubnetCommission(BigDecimal("0.003"))

    private var configuredCommission = commission

    private val configRepository = BittensorConfigRepository { BittensorConfig(configuredCommission, emptyMap()) }

    private var partial = false

    private var cancellation = false

    private var requestedInput: BigInteger? = null

    private val source = object : BittensorSwapSource {
        override suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorNetuid.Subnet): BigInteger = error("Unpinned price requested")

        override suspend fun quote(
            chainId: String,
            subnetId: BittensorNetuid.Subnet,
            direction: BittensorSwapDirection,
            amountIn: BigInteger
        ): BittensorSwapQuote {
            val simulation = simulation(subnetId, direction, amountIn)
            return BittensorSwapQuote("0xpinned", simulation, BigInteger.valueOf(1000))
        }

        override suspend fun simulate(
            chainId: String,
            subnetId: BittensorNetuid.Subnet,
            direction: BittensorSwapDirection,
            amountIn: BigInteger
        ): BittensorSwapSimulation = error("Unpinned simulation requested")

        private fun simulation(
            subnetId: BittensorNetuid.Subnet,
            direction: BittensorSwapDirection,
            amountIn: BigInteger
        ): BittensorSwapSimulation {
            if (cancellation) throw CancellationException("Quote replaced")
            requestedInput = amountIn
            val runtimeFee = BigInteger.TEN
            return BittensorSwapSimulation(
                subnetId,
                direction,
                amountIn - runtimeFee - if (partial) BigInteger.ONE else BigInteger.ZERO,
                BigInteger.valueOf(1234),
                runtimeFee,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO
            )
        }
    }

    @Test
    fun `Nova fee is included in total input and simulation receives only the stake amount`() = runBlocking {
        val quote = interactor().quoteBuy("bittensor", subnet, BigInteger.valueOf(1_000_000_000), slippage())
        assertEquals(BigInteger.valueOf(2_991_026), quote.serviceFeeTaoInPlanks)
        assertEquals(BigInteger.valueOf(997_008_974), requestedInput)
        assertEquals(requestedInput, quote.simulation.amountIn + quote.simulation.taoFee)
        assertEquals(quote.totalTaoInPlanks, quote.stakeTaoInPlanks + quote.serviceFeeTaoInPlanks)
        assertEquals(BigInteger.valueOf(1234), quote.alphaOutInPlanks) // Runtime swap fees must not be subtracted twice.
        assertEquals(BigInteger.valueOf(1005), quote.limitPrice)
    }

    @Test
    fun `tiny commission rounds down rather than charging a whole rao`() {
        assertEquals(BigInteger.ZERO, commission.feeFor(BigInteger.ONE))
        assertEquals(BigInteger.ZERO, commission.feeFor(BigInteger.valueOf(334)))
        assertEquals(BigInteger.ONE, commission.feeFor(BigInteger.valueOf(335)))
    }

    @Test
    fun `quotes capture the configured rate and later config changes do not alter confirmed fees`() = runBlocking {
        val original = interactor().quoteBuy("bittensor", subnet, BigInteger.valueOf(1_000_000_000), slippage())
        configuredCommission = BittensorSubnetCommission(BigDecimal("0.02"))
        val updated = interactor().quoteBuy("bittensor", subnet, BigInteger.valueOf(1_000_000_000), slippage())

        assertEquals(BigDecimal("0.003"), original.serviceFeeRate)
        assertEquals(BigInteger.valueOf(2_991_026), original.serviceFeeTaoInPlanks)
        assertEquals(BigDecimal("0.02"), updated.serviceFeeRate)
        assertEquals(BigInteger.valueOf(19_607_843), updated.serviceFeeTaoInPlanks)
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

    private fun interactor() = BittensorSubnetQuoteInteractor(source, configRepository)
    private fun slippage() = BittensorSlippage(0.005.toBigDecimal())
}
