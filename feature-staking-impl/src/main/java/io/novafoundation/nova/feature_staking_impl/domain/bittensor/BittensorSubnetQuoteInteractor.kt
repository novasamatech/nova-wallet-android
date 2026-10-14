package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * A buy quote denominated in atomic TAO/alpha units (10^9). The user's total TAO input
 * includes the Nova fee. The chain simulation already includes the chain's swap fee.
 * A quote is informational until its validator, wallet, fee recipient and balance are validated for submission.
 */
data class BittensorSubnetBuyQuote(
    val subnetId: BittensorSubnetId,
    val totalTaoInPlanks: BigInteger,
    val serviceFeeTaoInPlanks: BigInteger,
    val simulation: BittensorSwapSimulation,
    val limitPrice: BigInteger,
    val serviceFeeRate: BigDecimal
) {
    val stakeTaoInPlanks: BigInteger get() = totalTaoInPlanks - serviceFeeTaoInPlanks

    val alphaOutInPlanks: BigInteger get() = simulation.amountOut
}

class BittensorSubnetCommission(val fraction: BigDecimal) {

    init {
        require(fraction.isNonNegative && fraction < BigDecimal.ONE) { "Commission must be in [0, 1)" }
    }

    fun feeFor(taoAmount: BigInteger): BigInteger {
        require(taoAmount.isPositive() && taoAmount.bitLength() <= 64) { "Native-token amount must be a positive u64" }
        // Like iOS: the configured rate applies to the net amount, while the input includes the fee.
        return taoAmount.toBigDecimal().multiply(fraction).divide(BigDecimal.ONE + fraction, 0, RoundingMode.DOWN).toBigIntegerExact()
    }
}

class BittensorSubnetQuoteInteractor(
    private val swapSource: BittensorSwapSource,
    private val configRepository: BittensorConfigRepository
) {

    suspend fun quoteBuy(
        chainId: String,
        subnetId: BittensorSubnetId,
        totalTaoInPlanks: BigInteger,
        slippage: BittensorSlippage
    ): BittensorSubnetBuyQuote = withTimeout(20_000L) {
        val commission = configRepository.config().commission
        val serviceFee = commission.feeFor(totalTaoInPlanks)
        val stakeAmount = totalTaoInPlanks - serviceFee
        coroutineScope {
            val price = async { swapSource.currentAlphaPrice(chainId, subnetId) }
            val simulation = swapSource.simulate(chainId, subnetId, BittensorSwapDirection.TAO_TO_ALPHA, stakeAmount)
            require(simulation.subnetId == subnetId && simulation.direction == BittensorSwapDirection.TAO_TO_ALPHA) { "Unexpected subnet quote" }
            // amountIn is the runtime's net pool input. The swap fee is paid in the input token.
            require(simulation.amountIn + simulation.taoFee == stakeAmount) { "Partial subnet quotes are not supported" }
            require(simulation.amountOut.isPositive() && simulation.amountOut.bitLength() <= 64) { "Invalid subnet quote output" }

            BittensorSubnetBuyQuote(
                subnetId = subnetId,
                totalTaoInPlanks = totalTaoInPlanks,
                serviceFeeTaoInPlanks = serviceFee,
                simulation = simulation,
                limitPrice = slippage.limitPrice(price.await(), BittensorSwapDirection.TAO_TO_ALPHA),
                serviceFeeRate = commission.fraction
            )
        }
    }
}
