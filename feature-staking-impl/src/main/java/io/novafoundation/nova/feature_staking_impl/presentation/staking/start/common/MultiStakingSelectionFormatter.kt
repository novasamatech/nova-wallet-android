package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common

import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.isRecommended
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel

interface MultiStakingSelectionFormatter {

    suspend fun formatForSetupAmount(recommendableSelection: RecommendableMultiStakingSelection): StakingTargetModel

    suspend fun formatForStakingType(recommendableSelection: RecommendableMultiStakingSelection): StakingTargetModel
}

class RealMultiStakingSelectionFormatter(
    private val resourceManager: ResourceManager,
    private val poolDisplayFormatter: PoolDisplayFormatter,
) : MultiStakingSelectionFormatter {

    override suspend fun formatForSetupAmount(
        recommendableSelection: RecommendableMultiStakingSelection,
    ): StakingTargetModel {
        return when (val selection = recommendableSelection.selection) {
            is DirectStakingSelection -> StakingTargetModel(
                title = resourceManager.getString(R.string.setup_staking_type_direct_staking),
                subtitle = formatValidatorsSubtitle(recommendableSelection, selection),
                icon = null
            )

            is NominationPoolSelection -> StakingTargetModel(
                title = resourceManager.getString(R.string.setup_staking_type_pool_staking),
                subtitle = formatSubtitle(recommendableSelection) {
                    poolDisplayFormatter.formatTitle(selection.pool, selection.stakingOption.chain)
                },
                icon = null
            )

            else -> notYetImplemented(selection)
        }
    }

    override suspend fun formatForStakingType(
        recommendableSelection: RecommendableMultiStakingSelection,
    ): StakingTargetModel {
        return when (val selection = recommendableSelection.selection) {
            is DirectStakingSelection -> StakingTargetModel(
                title = resourceManager.getString(R.string.staking_recommended_title),
                subtitle = formatValidatorsSubtitle(recommendableSelection, selection),
                icon = null
            )

            is NominationPoolSelection -> {
                val poolDisplay = poolDisplayFormatter.format(selection.pool, selection.stakingOption.chain)

                StakingTargetModel(
                    title = poolDisplay.title,
                    subtitle = recommendedSubtitle(recommendableSelection),
                    icon = poolDisplay.icon.asStakeTargetIcon()
                )
            }

            else -> notYetImplemented(selection)
        }
    }

    private fun formatValidatorsSubtitle(
        recommendableSelection: RecommendableMultiStakingSelection,
        selection: DirectStakingSelection
    ) = formatSubtitle(recommendableSelection) {
        resourceManager.getString(R.string.start_staking_selection_validators_subtitle, selection.validators.size, selection.validatorsLimit)
    }

    private fun recommendedSubtitle(selection: RecommendableMultiStakingSelection) = formatSubtitle(selection, notRecommendedText = { null })

    private fun formatSubtitle(selection: RecommendableMultiStakingSelection, notRecommendedText: () -> String?): ColoredText? {
        return if (selection.source.isRecommended) {
            ColoredText(
                text = resourceManager.getString(R.string.common_recommended),
                colorRes = R.color.text_positive
            )
        } else {
            notRecommendedText()?.let {
                ColoredText(
                    text = it,
                    colorRes = R.color.text_tertiary
                )
            }
        }
    }

    private fun notYetImplemented(selection: StartMultiStakingSelection): Nothing {
        error("Not yet implemented: ${selection::class.simpleName}")
    }

    private fun Icon?.asStakeTargetIcon(): StakingTargetModel.TargetIcon? = this?.let(StakingTargetModel.TargetIcon::Icon)
}
