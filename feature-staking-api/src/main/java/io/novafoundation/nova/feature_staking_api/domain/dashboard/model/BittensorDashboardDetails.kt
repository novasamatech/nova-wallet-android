package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import java.math.BigDecimal
import java.math.BigInteger

/** Rates are annual percentages; amounts are native-token planks. Null never means zero. */
data class BittensorDashboardDetails(
    val totalStakeInPlanks: BigInteger?,
    val rootStakeInPlanks: BigInteger?,
    val subnetCount: Int,
    val rootAnnualPercentageYield: BigDecimal?,
    val subnetAnnualPercentageYield: BigDecimal?,
    val maximumAnnualPercentageYield: BigDecimal?,
    val pricing: Pricing,
    val rootYieldLoading: Boolean = false,
    val subnetYieldLoading: Boolean = false,
    val maximumYieldLoading: Boolean = false,
    /** Root rewards meeting the claim minimum. */
    val claimableRootRewardsInPlanks: BigInteger? = null
) {

    enum class Pricing { LOADING, KNOWN, UNKNOWN }

    enum class Composition { NONE, ROOT, SUBNETS, MIXED }

    val composition: Composition
        get() = when {
            rootStakeInPlanks != null && subnetCount > 0 -> Composition.MIXED
            rootStakeInPlanks != null -> Composition.ROOT
            subnetCount > 0 -> Composition.SUBNETS
            else -> Composition.NONE
        }

    /** Nothing is staked, but root rewards are still claimable: the panel shows those rewards. */
    val showsClaimableRewardsPanel: Boolean
        get() = composition == Composition.NONE && claimableRootRewardsInPlanks?.signum() == 1

    val showsRootPanel: Boolean
        get() = rootStakeInPlanks != null || subnetCount == 0

    val panelStakeInPlanks: BigInteger?
        get() = when {
            showsClaimableRewardsPanel -> claimableRootRewardsInPlanks
            showsRootPanel -> rootStakeInPlanks
            else -> totalStakeInPlanks
        }

    val panelAnnualPercentageYield: BigDecimal?
        get() = when {
            showsClaimableRewardsPanel -> null
            showsRootPanel -> rootAnnualPercentageYield
            else -> subnetAnnualPercentageYield
        }

    val panelYieldLoading: Boolean
        get() = when {
            showsClaimableRewardsPanel -> false
            showsRootPanel -> rootYieldLoading
            else -> subnetYieldLoading
        }
}
