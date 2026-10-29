package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal fun loadingBittensorDashboardDetails(): BittensorDashboardDetails {
    return BittensorDashboardDetails(
        totalStake = StakingDashboardValue.Loading,
        rootStakeInPlanks = null,
        subnetCount = 0,
        rootAnnualPercentageYield = StakingDashboardValue.Loading,
        subnetAnnualPercentageYield = StakingDashboardValue.Loading,
        maximumAnnualPercentageYield = StakingDashboardValue.Loading
    )
}

internal fun mapBittensorDashboardDetails(
    positions: BittensorPortfolioPositions?,
    rootYields: ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>,
    subnetYields: Map<BittensorNetuid.Subnet, ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>>,
    maximumAnnualPercentageYield: StakingDashboardValue<BigDecimal> = StakingDashboardValue.Unavailable,
    pricingPending: Boolean = positions == null,
    claimableRootRewards: BigInteger? = null
): BittensorDashboardDetails {
    return BittensorDashboardDetails(
        totalStake = mapStakeValuation(positions, pricingPending),
        rootStakeInPlanks = mapRootPanelStake(positions, claimableRootRewards),
        subnetCount = positions?.subnets?.map { it.subnetId }?.distinct()?.size ?: 0,
        rootAnnualPercentageYield = mapRootYield(positions, rootYields),
        subnetAnnualPercentageYield = mapSubnetYield(positions, subnetYields, pricingPending),
        maximumAnnualPercentageYield = maximumAnnualPercentageYield,
        claimableRootRewardsInPlanks = claimableRootRewards
    )
}

private fun mapRootPanelStake(positions: BittensorPortfolioPositions?, claimableRootRewards: BigInteger?): BigInteger? {
    // Rewards add Root to a subnet stake; without any stake they get their own panel.
    return when {
        positions == null -> null
        positions.root.isNotEmpty() -> positions.rootStake
        positions.subnets.isNotEmpty() && claimableRootRewards?.isPositive() == true -> BigInteger.ZERO
        else -> null
    }
}

private fun mapRootYield(
    positions: BittensorPortfolioPositions?,
    yields: ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>
): StakingDashboardValue<BigDecimal> {
    if (yields is ExtendedLoadingState.Loading) return StakingDashboardValue.Loading
    if (positions == null) return StakingDashboardValue.Unavailable

    val loadedYields = yields.dataOrNull.orEmpty()
    val rates = positions.root.map { WeightedRate(loadedYields[it.validatorHotkey], it.stakeInPlanks) }
    return weightedDashboardRate(rates)
}

private fun mapSubnetYield(
    positions: BittensorPortfolioPositions?,
    yields: Map<BittensorNetuid.Subnet, ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>>,
    pricingPending: Boolean
): StakingDashboardValue<BigDecimal> {
    val anyYieldLoading = yields.values.any { it is ExtendedLoadingState.Loading }
    if (pricingPending || anyYieldLoading) return StakingDashboardValue.Loading
    if (positions == null) return StakingDashboardValue.Unavailable

    val rates = positions.subnets.map { position ->
        val price = positions.subnetPrices[position.subnetId]
        val stakeInRao = price?.let { position.alphaInPlanks * it / BittensorProtocolUnits.RAO_PER_TAO }
        val yield = yields[position.subnetId]?.dataOrNull?.get(position.validatorHotkey)
        WeightedRate(yield, stakeInRao)
    }
    return weightedDashboardRate(rates)
}

private fun mapStakeValuation(positions: BittensorPortfolioPositions?, pricingPending: Boolean): StakingDashboardValue<BigInteger> {
    if (pricingPending) return StakingDashboardValue.Loading
    val valuation = positions?.valuation ?: return StakingDashboardValue.Unavailable
    return if (valuation.isComplete) StakingDashboardValue.Known(valuation.amount) else StakingDashboardValue.Unavailable
}

internal fun maximumYield(recommendedYields: Map<BittensorNetuid.Subnet, BigDecimal>): StakingDashboardValue<BigDecimal> {
    val maximum = recommendedYields.values.filter { it.isNonNegative }.maxOrNull()
    return maximum?.let { StakingDashboardValue.Known(it) } ?: StakingDashboardValue.Unavailable
}

private data class WeightedRate(val annualPercentageYield: BigDecimal?, val stake: BigInteger?)

private fun weightedDashboardRate(values: List<WeightedRate>): StakingDashboardValue<BigDecimal> {
    var totalStake = BigDecimal.ZERO
    var weightedRate = BigDecimal.ZERO
    for (value in values) {
        val rate = value.annualPercentageYield?.takeIf { it.isNonNegative } ?: return StakingDashboardValue.Unavailable
        val stake = value.stake?.toBigDecimal() ?: return StakingDashboardValue.Unavailable
        totalStake += stake
        weightedRate += rate * stake
    }
    if (!totalStake.isPositive) return StakingDashboardValue.Unavailable
    return StakingDashboardValue.Known(weightedRate.divide(totalStake, 12, RoundingMode.HALF_EVEN))
}
