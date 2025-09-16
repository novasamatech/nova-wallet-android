package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.presentation.masking.unfoldHidden
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.appendEnd
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.formatting.spannable.format
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake.FlowType
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NotYetResolved
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.WithoutStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingPrimary
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingSecondary
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.StakingTypeModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.syncingIf
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType
import io.novasama.substrate_sdk_android.hash.isPositive

class StakingDashboardPresentationMapperFactory(
    private val resourceManager: ResourceManager
) {
    fun create(maskableValueFormatter: MaskableValueFormatter): StakingDashboardPresentationMapper {
        return RealStakingDashboardPresentationMapper(resourceManager, maskableValueFormatter)
    }
}

interface StakingDashboardPresentationMapper {

    fun mapWithoutStakeItemToUi(withoutStake: AggregatedStakingDashboardOption<WithoutStake>): StakingDashboardModel.NoStakeItem

    fun mapStakingTypeToUi(stakingType: StakingType): StakingTypeModel
}

class RealStakingDashboardPresentationMapper(
    private val resourceManager: ResourceManager,
    private val maskableValueFormatter: MaskableValueFormatter
) : StakingDashboardPresentationMapper {

    @Suppress("UNCHECKED_CAST")
    override fun mapWithoutStakeItemToUi(
        withoutStake: AggregatedStakingDashboardOption<WithoutStake>
    ): StakingDashboardModel.NoStakeItem {
        return when (withoutStake.stakingState) {
            is NoStake -> mapNoStakeItemToUi(withoutStake as AggregatedStakingDashboardOption<NoStake>)
            NotYetResolved -> mapNotYetResolvedItemToUi(withoutStake as AggregatedStakingDashboardOption<NotYetResolved>)
        }
    }

    override fun mapStakingTypeToUi(stakingType: StakingType): StakingTypeModel {
        return if (stakingType == StakingType.NOMINATION_POOLS) {
            StakingTypeModel(
                icon = R.drawable.ic_nomination_pool,
                text = resourceManager.getString(R.string.nomination_pools_pool)
            )
        } else {
            StakingTypeModel(
                icon = R.drawable.ic_nominator,
                text = resourceManager.getString(R.string.nomination_pools_direct)
            )
        }
    }

    private fun mapNotYetResolvedItemToUi(noStake: AggregatedStakingDashboardOption<NotYetResolved>): StakingDashboardModel.NoStakeItem {
        return StakingDashboardModel.NoStakeItem(
            chainUi = mapChainToUi(noStake.chain).syncingIf(isSyncing = true),
            assetId = noStake.token.configuration.id,
            earnings = ExtendedLoadingState.Loading,
            availableBalance = null,
            stakingTypeBadge = null,
        )
    }

    private fun mapNoStakeItemToUi(noStake: AggregatedStakingDashboardOption<NoStake>): StakingDashboardModel.NoStakeItem {
        val stats = noStake.stakingState.stats
        val syncingStage = noStake.syncingStage

        val availableBalance = noStake.stakingState.availableBalance
        val formattedAvailableBalance = if (availableBalance.isPositive()) {
            val maskableValue = maskableValueFormatter.format<CharSequence> { availableBalance.formatPlanks(noStake.token.configuration) }
                .unfoldHidden {
                    val maskingDrawable = resourceManager.getDrawable(R.drawable.mask_dots_small)
                    SpannableStringBuilder()
                        .append(" ") // Small space before masking
                        .appendEnd(drawableSpan(maskingDrawable, extendToLineHeight = true))
                }

            SpannableFormatter.format(resourceManager, R.string.common_available_format, maskableValue)
        } else {
            null
        }

        val stakingType = noStake.stakingState.flowType.displayableStakingType()

        return StakingDashboardModel.NoStakeItem(
            chainUi = mapChainToUi(noStake.chain).syncingIf(syncingStage.isSyncingPrimary()),
            assetId = noStake.token.configuration.id,
            earnings = stats.map { it.estimatedEarnings.format().syncingIf(syncingStage.isSyncingSecondary()) },
            availableBalance = formattedAvailableBalance,
            stakingTypeBadge = stakingType?.let(::mapStakingTypeToUi)
        )
    }

    private fun FlowType.displayableStakingType(): StakingType? {
        return when (this) {
            is FlowType.Aggregated -> null
            is FlowType.Single -> stakingType.takeIf { showStakingType }
        }
    }
}
