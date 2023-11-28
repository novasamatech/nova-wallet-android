package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingTargetSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.isDirectStaking
import io.novafoundation.nova.runtime.ext.isPoolStaking

class EditableStakingTypeItemFormatter(
    private val resourceManager: ResourceManager,
    private val multiStakingTargetSelectionFormatter: MultiStakingTargetSelectionFormatter
) {

    suspend fun format(
        asset: Asset,
        validatedStakingType: ValidatedStakingTypeDetails,
        selection: RecommendableMultiStakingSelection
    ): EditableStakingTypeRVItem? {
        val stakingTarget = multiStakingTargetSelectionFormatter.formatForStakingType(selection)
        val selectedStakingType = selection.selection.stakingOption.stakingType
        val stakingType = validatedStakingType.stakingTypeDetails.stakingType

        val (titleRes, imageRes) = when {
            stakingType.isDirectStaking() -> R.string.setup_staking_type_direct_staking to R.drawable.ic_direct_staking_banner_picture
            stakingType.isPoolStaking() -> R.string.setup_staking_type_pool_staking to R.drawable.ic_pool_staking_banner_picture
            else -> return null
        }

        val isSelected = selectedStakingType == stakingType

        return EditableStakingTypeRVItem(
            isSelected = isSelected,
            isSelectable = validatedStakingType.isAvailable || isSelected,
            title = resourceManager.getString(titleRes),
            imageRes = imageRes,
            conditions = mapConditions(asset, validatedStakingType.stakingTypeDetails),
            stakingTarget = stakingTarget.takeIf { selectedStakingType == stakingType }
        )
    }

    private fun mapConditions(asset: Asset, stakingTypeDetails: StakingTypeDetails): List<String> {
        return buildList {
            val minAmount = mapAmountToAmountModel(stakingTypeDetails.minStake, asset.token)
            add(resourceManager.getString(R.string.setup_staking_type_min_amount_condition, minAmount.token))

            val payoutCondition = when (stakingTypeDetails.payoutType) {
                is PayoutType.Automatically -> resourceManager.getString(R.string.setup_staking_type_payout_type_automatically_condition)
                is PayoutType.Manual -> resourceManager.getString(R.string.setup_staking_type_payout_type_manual_condition)
            }
            add(payoutCondition)

            if (stakingTypeDetails.participationInGovernance) {
                add(resourceManager.getString(R.string.setup_staking_type_governance_condition))
            }

            if (stakingTypeDetails.advancedOptionsAvailable) {
                add(resourceManager.getString(R.string.setup_staking_type_advanced_options_condition))
            }
        }
    }
}
