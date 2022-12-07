package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

class AlephZeroRewardCalculator(
    validators: List<RewardCalculationTarget>,
    yearlyMint: BigInteger,
) : RewardCalculator {

    private val totalStake = validators.sumByBigInteger { it.totalStake }

    private val apr = yearlyMint.toDouble() / totalStake.toDouble()

    private var apyByValidator = validators.associateBy(
        keySelector = { it.accountIdHex },
        valueTransform = { calculateValidatorApy(it.commission) }
    )

    override val expectedAPY: BigDecimal = apyByValidator.values
        .average()
        .toBigDecimal()

    override val maxAPY: Double = apyByValidator.values.max()

    override fun getApyFor(targetIdHex: String): BigDecimal {
        return apyByValidator[targetIdHex]?.toBigDecimal() ?: expectedAPY
    }

    private fun calculateValidatorApy(validatorCommission: BigDecimal): Double {
        val validatorApr = apr * (1 - validatorCommission.toDouble())

        return aprToApy(validatorApr)
    }
}

@Suppress("FunctionName")
fun AlephZeroRewardCalculator(
    validators: List<RewardCalculationTarget>,
    chainAsset: Chain.Asset
): RewardCalculator {
    // https://github.com/Cardinal-Cryptography/aleph-node/blob/5acf27dc475767134aeb29b0681768ab93435101/primitives/src/lib.rs#L228
    val yearlyTotalMint = 30_000_000
    val yearlyStakingMint = yearlyTotalMint * 0.9 // 10% goes to treasury

    val yearlyMintPlanks = chainAsset.planksFromAmount(yearlyStakingMint.toBigDecimal())

    return AlephZeroRewardCalculator(
        validators = validators,
        yearlyMint = yearlyMintPlanks
    )
}
