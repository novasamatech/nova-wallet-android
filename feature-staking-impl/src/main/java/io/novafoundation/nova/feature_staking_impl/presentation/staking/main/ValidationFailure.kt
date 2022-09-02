package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.handleChainAccountNotFound
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter

fun welcomeStakingValidationFailure(
    reason: WelcomeStakingValidationFailure,
    resourceManager: ResourceManager,
    router: StakingRouter
): TransformedFailure = with(resourceManager) {
    when (reason) {
        WelcomeStakingValidationFailure.MaxNominatorsReached -> {
            TransformedFailure.Default(
                getString(R.string.staking_max_nominators_reached_title)
                    to getString(R.string.staking_max_nominators_reached_message)
            )
        }

        is WelcomeStakingValidationFailure.NoRelayChainAccount -> handleChainAccountNotFound(
            failure = reason,
            resourceManager = resourceManager,
            goToWalletDetails = { router.openAccountDetails(reason.account.id) }
        )
    }
}

fun mainStakingValidationFailure(
    reason: StakeActionsValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage = with(resourceManager) {
    when (reason) {
        is StakeActionsValidationFailure.ControllerRequired -> {
            getString(R.string.common_error_general_title) to
                getString(R.string.staking_add_controller, reason.controllerAddress)
        }

        is StakeActionsValidationFailure.UnbondingRequestLimitReached -> {
            getString(R.string.staking_unbonding_limit_reached_title) to
                getString(R.string.staking_unbonding_limit_reached_message, reason.limit)
        }
        is StakeActionsValidationFailure.StashRequired -> {
            getString(R.string.common_error_general_title) to
                getString(R.string.staking_stash_missing_message, reason.stashAddress)
        }
    }
}
