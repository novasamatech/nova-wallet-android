package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal fun mapBittensorDashboardDetails(
    positions: BittensorPortfolioPositions?,
    rootYields: Map<AccountIdKey, BigDecimal>,
    subnetYields: Map<BittensorNetuid.Subnet, Map<AccountIdKey, BigDecimal>>,
    recommendedYields: Map<BittensorNetuid.Subnet, BigDecimal>,
    pricingPending: Boolean = positions == null
): BittensorDashboardDetails {
    val rootStake = positions?.rootStake?.takeIf { positions.root.isNotEmpty() }
    val rootRate = positions?.root?.let { roots ->
        weightedDashboardRate(roots.map { WeightedRate(rootYields[it.validatorHotkey], it.stakeInPlanks) })
    }
    val subnetRate = positions?.subnets?.let { subnets ->
        weightedDashboardRate(
            subnets.map { position ->
                val price = positions.subnetPrices[position.subnetId]
                val value = price?.let { position.alphaInPlanks * it / BittensorProtocolUnits.RAO_PER_TAO }
                WeightedRate(subnetYields[position.subnetId]?.get(position.validatorHotkey), value)
            }
        )
    }
    val pricing = when {
        pricingPending -> BittensorDashboardDetails.Pricing.LOADING
        positions?.totalStake == null -> BittensorDashboardDetails.Pricing.UNKNOWN
        else -> BittensorDashboardDetails.Pricing.KNOWN
    }
    return BittensorDashboardDetails(
        totalStakeInPlanks = positions?.totalStake.takeUnless { pricingPending },
        rootStakeInPlanks = rootStake,
        subnetCount = positions?.subnets?.map { it.subnetId }?.distinct()?.size ?: 0,
        rootAnnualPercentageYield = rootRate,
        subnetAnnualPercentageYield = subnetRate.takeUnless { pricingPending },
        maximumAnnualPercentageYield = recommendedYields.values.filter { it.isNonNegative }.maxOrNull(),
        pricing = pricing
    )
}

private data class WeightedRate(val annualPercentageYield: BigDecimal?, val stake: BigInteger?)

private fun weightedDashboardRate(values: List<WeightedRate>): BigDecimal? {
    var totalStake = BigDecimal.ZERO
    var weightedRate = BigDecimal.ZERO
    for (value in values) {
        val rate = value.annualPercentageYield?.takeIf { it.isNonNegative } ?: return null
        val stake = value.stake?.toBigDecimal() ?: return null
        totalStake += stake
        weightedRate += rate * stake
    }
    if (!totalStake.isPositive) return null
    return weightedRate.divide(totalStake, 12, RoundingMode.HALF_EVEN)
}
