package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.ext.isDirectStaking
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain


fun handleSetupStakingTypeValidationFailure(chainAsset: Chain.Asset, error: EditingStakingTypeFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (error) {
        is EditingStakingTypeFailure.AmountIsLessThanMinStake -> {
            val amount = error.minStake.formatPlanks(chainAsset)
            when {
                error.stakingType.isDirectStaking() -> TitleAndMessage(
                    resourceManager.getString(R.string.setup_staking_type_staking_amount_is_less_than_min_amount_title),
                    resourceManager.getString(R.string.setup_staking_type_direct_staking_amount_is_less_than_min_amount_message, amount)
                )
                else -> TitleAndMessage(
                    resourceManager.getString(R.string.setup_staking_type_staking_amount_is_less_than_min_amount_title),
                    resourceManager.getString(R.string.setup_staking_type_staking_amount_is_less_than_min_amount_fallback_message, amount)
                )
            }
        }

        is EditingStakingTypeFailure.StakingTypeIsAlreadyUsing -> {
            return when (error.stakingType.group()) {
                StakingTypeGroup.PARACHAIN,
                StakingTypeGroup.RELAYCHAIN -> TitleAndMessage(
                    resourceManager.getString(R.string.setup_staking_type_already_used_title),
                    resourceManager.getString(R.string.setup_staking_type_direct_already_used_message)
                )
                StakingTypeGroup.NOMINATION_POOL -> TitleAndMessage(
                    resourceManager.getString(R.string.setup_staking_type_already_used_title),
                    resourceManager.getString(R.string.setup_staking_type_pool_already_used_message)
                )
                StakingTypeGroup.UNSUPPORTED -> TitleAndMessage(
                    resourceManager.getString(R.string.setup_staking_type_already_used_title),
                    resourceManager.getString(R.string.setup_staking_type_unsupported_staking_type_used_fallback_message)
                )
            }
        }
    }
}
