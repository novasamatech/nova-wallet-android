package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake.StakingStatus
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails.Composition
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isKnownAndZero
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingPrimary
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.announcements.AnnouncementModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.SyncingData
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.syncingIf
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigInteger
import io.novafoundation.nova.runtime.ext.fullId

internal fun mapBittensorDashboardItem(
    option: AggregatedStakingDashboardOption<AggregatedStakingDashboardOption.HasStake>,
    amountFormatter: AmountFormatter,
    announcement: AnnouncementModel?,
    resources: ResourceManager,
    masking: MaskableValueFormatter,
    icons: AssetIconProvider
): StakingDashboardModel.HasStakeItem? {
    val details = option.bittensorDetails ?: return null
    val pending = details.totalStake is StakingDashboardValue.Loading
    val total = masking.format {
        val amount = mapStakeAmount(details.totalStake, amountFormatter, option.token)
        if (details.subnetCount > 0) amount.copy(fiat = amount.fiat?.let { resources.getString(R.string.bittensor_dashboard_estimated, it) }) else amount
    }
    val panel = masking.format {
        mapStakeAmount(details.panelStake, amountFormatter, option.token)
    }
    val rootTitle = resources.getString(R.string.bittensor_dashboard_root)
    val subnetTitle = if (details.subnetCount == 1) {
        resources.getString(R.string.bittensor_dashboard_one_subnet)
    } else {
        resources.getString(R.string.bittensor_dashboard_subnets, details.subnetCount)
    }
    val composition = when (details.composition) {
        Composition.ROOT -> rootTitle
        Composition.SUBNETS -> subnetTitle
        Composition.MIXED -> resources.getString(R.string.bittensor_dashboard_mixed, subnetTitle)
        Composition.NONE -> null
    }
    val label = composition?.let { resources.getString(R.string.bittensor_dashboard_title, option.token.configuration.name, it) }
        ?: option.token.configuration.name
    val panelTitle = when {
        details.showsClaimableRewardsPanel -> resources.getString(R.string.bittensor_claim_available)
        details.showsRootPanel -> rootTitle
        details.subnetCount == 1 -> subnetTitle
        else -> resources.getString(R.string.bittensor_dashboard_subnets_title)
    }
    // Like iOS hasActiveStaking: this card exists for positions or claimable root rewards.
    // Root-validator registration is a separate status on the root position screen.
    val status = mapDashboardStakingStatus(StakingStatus.ACTIVE, resources)
    return StakingDashboardModel.HasStakeItem(
        assetLabel = label.syncingIf(pending || option.syncingStage.isSyncingPrimary()),
        assetId = option.token.configuration.fullId,
        stakingTypeBadge = null,
        assetIcon = icons.getAssetIconOrFallback(option.token.configuration.icon).syncingIf(option.syncingStage.isSyncingPrimary()),
        rewards = if (pending) ExtendedLoadingState.Loading else ExtendedLoadingState.Loaded(total.syncingIf(false)),
        stake = panel.syncingIf(pending),
        status = ExtendedLoadingState.Loaded(status.syncingIf(option.syncingStage.isSyncingPrimary())),
        earnings = mapBittensorPanelYield(details),
        announcement = announcement,
        stakeTitle = panelTitle
    )
}

internal fun mapBittensorPanelYield(details: BittensorDashboardDetails): ExtendedLoadingState<SyncingData<String>> {
    val pricingPending = details.totalStake is StakingDashboardValue.Loading
    val rate = details.panelAnnualPercentageYield
    val yieldPending = rate is StakingDashboardValue.Loading && !details.panelStake.isKnownAndZero()
    if (pricingPending || yieldPending) return ExtendedLoadingState.Loading

    val value = when (rate) {
        is StakingDashboardValue.Known -> Percent(rate.value.toDouble()).format()
        StakingDashboardValue.Loading, StakingDashboardValue.Unavailable -> "—"
    }
    return ExtendedLoadingState.Loaded(value.syncingIf(false))
}

private fun mapStakeAmount(valuation: StakingDashboardValue<BigInteger>, amountFormatter: AmountFormatter, token: Token): AmountModel {
    return when (valuation) {
        is StakingDashboardValue.Known -> amountFormatter.formatAmountToAmountModel(valuation.value, token)
        StakingDashboardValue.Loading, StakingDashboardValue.Unavailable -> AmountModel(token = "—", fiat = null)
    }
}

internal fun mapBittensorMaximumYield(
    details: BittensorDashboardDetails,
    resources: ResourceManager
): ExtendedLoadingState<SyncingData<String>> {
    val value = when (val rate = details.maximumAnnualPercentageYield) {
        StakingDashboardValue.Loading -> return ExtendedLoadingState.Loading
        StakingDashboardValue.Unavailable -> "—"
        is StakingDashboardValue.Known -> resources.getString(R.string.bittensor_dashboard_max_apy, Percent(rate.value.toDouble()).format())
    }
    return ExtendedLoadingState.Loaded(value.syncingIf(false))
}
