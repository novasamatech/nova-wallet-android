package io.novafoundation.nova.feature_staking_impl.domain.rewards

import java.math.BigInteger
import kotlin.math.pow

class InflationConfig(
    val falloff: Double,
    val maxInflation: Double,
    val minInflation: Double,
    val stakeTarget: Double,
    val parachainAdjust: ParachainAdjust?
) {

    class ParachainAdjust(
        val maxParachains: Int,
        val activePublicParachains: Int,
        val parachainReservedSupplyFraction: Double
    )

    companion object {

        // defaults based on Kusama runtime
        fun Default(activePublicParachains: Int?) = InflationConfig(
            falloff = 0.05,
            maxInflation = 0.1,
            minInflation = 0.025,
            stakeTarget = 0.75,
            parachainAdjust = activePublicParachains?.let {
                ParachainAdjust(
                    maxParachains = 60,
                    activePublicParachains = activePublicParachains,
                    parachainReservedSupplyFraction = 0.3
                )
            }
        )

        // Polkadot has different `parachainReservedSupplyFraction`
        fun Polkadot(activePublicParachains: Int?) = InflationConfig(
            falloff = 0.05,
            maxInflation = 0.1,
            minInflation = 0.025,
            stakeTarget = 0.75,
            parachainAdjust = activePublicParachains?.let {
                ParachainAdjust(
                    maxParachains = 60,
                    activePublicParachains = activePublicParachains,
                    parachainReservedSupplyFraction = 0.2
                )
            }
        )
    }
}

private fun InflationConfig.idealStake(): Double {
    val parachainAdjust = if (parachainAdjust != null) {
        with(parachainAdjust) {
            val cappedActiveParachains = activePublicParachains.coerceAtMost(maxParachains)

            cappedActiveParachains.toDouble() / maxParachains * parachainReservedSupplyFraction
        }
    } else {
        0.0
    }

    return stakeTarget - parachainAdjust
}

class RewardCurveInflationRewardCalculator(
    validators: List<RewardCalculationTarget>,
    totalIssuance: BigInteger,
    private val inflationConfig: InflationConfig,
) : InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double = with(inflationConfig) {
        val idealStake = idealStake()
        val idealInterest = maxInflation / idealStake

        val inflation = inflationConfig.minInflation + if (stakedPortion in 0.0..idealStake) {
            stakedPortion * (idealInterest - minInflation / idealStake)
        } else {
            (idealInterest * idealStake - minInflation) * 2.0.pow((idealStake - stakedPortion) / falloff)
        }

        inflation
    }
}
