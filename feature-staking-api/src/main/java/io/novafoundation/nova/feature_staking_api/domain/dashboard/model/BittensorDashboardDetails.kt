package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger

/** Rates are annual percentages; amounts are native-token planks. Null never means zero. */
data class BittensorDashboardDetails(
    val totalStake: StakingDashboardValue<BigInteger>,
    /** Null when no root component is known; zero can represent root rewards alongside subnet positions. */
    val rootStakeInPlanks: BigInteger?,
    val subnetCount: Int,
    val rootAnnualPercentageYield: StakingDashboardValue<BigDecimal>,
    val subnetAnnualPercentageYield: StakingDashboardValue<BigDecimal>,
    val maximumAnnualPercentageYield: StakingDashboardValue<BigDecimal>,
    /** Root rewards meeting the claim minimum. */
    val claimableRootRewardsInPlanks: BigInteger? = null
) {

    enum class Composition { NONE, ROOT, SUBNETS, MIXED }

    /** Known position composition; NONE does not imply that loading has completed. */
    val composition: Composition
        get() = when {
            rootStakeInPlanks != null && subnetCount > 0 -> Composition.MIXED
            rootStakeInPlanks != null -> Composition.ROOT
            subnetCount > 0 -> Composition.SUBNETS
            else -> Composition.NONE
        }

    /** Nothing is staked, but root rewards are still claimable: the panel shows those rewards. */
    val showsClaimableRewardsPanel: Boolean
        get() = composition == Composition.NONE && claimableRootRewardsInPlanks?.isPositive() == true

    val showsRootPanel: Boolean
        get() = rootStakeInPlanks != null || subnetCount == 0

    val panelStake: StakingDashboardValue<BigInteger>
        get() = when {
            showsClaimableRewardsPanel -> StakingDashboardValue.Known(requireNotNull(claimableRootRewardsInPlanks))
            showsRootPanel -> rootStakeInPlanks?.let { StakingDashboardValue.Known(it) }
                ?: if (totalStake is StakingDashboardValue.Loading) StakingDashboardValue.Loading else StakingDashboardValue.Unavailable
            else -> totalStake
        }

    val panelAnnualPercentageYield: StakingDashboardValue<BigDecimal>
        get() = when {
            showsClaimableRewardsPanel -> StakingDashboardValue.Unavailable
            showsRootPanel -> rootAnnualPercentageYield
            else -> subnetAnnualPercentageYield
        }
}
