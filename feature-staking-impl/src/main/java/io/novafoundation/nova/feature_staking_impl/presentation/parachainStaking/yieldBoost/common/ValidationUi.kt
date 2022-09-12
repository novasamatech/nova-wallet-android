package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.common

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationFailure.FirstTaskCannotExecute.Type.EXECUTION_FEE
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationFailure.FirstTaskCannotExecute.Type.THRESHOLD
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount

fun yieldBoostValidationFailure(failure: YieldBoostValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (failure) {
        is YieldBoostValidationFailure.FirstTaskCannotExecute -> {
            val (titleRes, messageRes) = when (failure.type) {
                THRESHOLD -> R.string.yield_boost_not_enough_threshold_title to R.string.yield_boost_not_enough_threshold_message
                EXECUTION_FEE -> R.string.yield_boost_not_enough_execution_fee_title to R.string.yield_boost_not_enough_execution_fee_message
            }

            val networkFee = failure.networkFee.formatTokenAmount(failure.chainAsset)
            val minimumRequired = failure.minimumBalanceRequired.formatTokenAmount(failure.chainAsset)
            val available = failure.availableBalanceBeforeFees.formatTokenAmount(failure.chainAsset)

            resourceManager.getString(titleRes) to resourceManager.getString(messageRes, networkFee, minimumRequired, available)
        }

        is YieldBoostValidationFailure.NotEnoughToPayToPayFees -> handleNotEnoughFeeError(failure, resourceManager)

        is YieldBoostValidationFailure.WillCancelAllExistingTasks -> {
            val collatorName = failure.newCollator.identity?.display ?: failure.newCollator.address

            resourceManager.getString(R.string.yield_boost_already_enabled_title) to
                resourceManager.getString(R.string.yield_boost_already_enabled_message, collatorName)
        }
    }
}
