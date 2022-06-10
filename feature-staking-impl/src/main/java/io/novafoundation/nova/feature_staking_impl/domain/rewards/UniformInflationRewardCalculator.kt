package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class UniformInflationRewardCalculator(
    validators: List<RewardCalculationTarget>,
    private val totalIssuance: BigInteger,
    private val yearlyMint: BigInteger
) : InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double {
        return yearlyMint.toDouble() / totalIssuance.toDouble()
    }
}

@Suppress("FunctionName")
fun AlephZeroRewardCalculator(
    validators: List<RewardCalculationTarget>,
    totalIssuance: BigInteger,
    chainAsset: Chain.Asset
): RewardCalculator {
    // https://github.com/Cardinal-Cryptography/aleph-node/blob/830088fdbdd7cce72d5eff9642c9961762c9e251/primitives/src/lib.rs#L75
    val yearlyTotalMint = 30_000_000
    val yearlyStakingMint = yearlyTotalMint * 0.9 // 10% goes to treasury

    val yearlyMintPlanks = chainAsset.planksFromAmount(yearlyStakingMint.toBigDecimal())

    return UniformInflationRewardCalculator(
        validators = validators,
        totalIssuance = totalIssuance,
        yearlyMint = yearlyMintPlanks
    )
}
