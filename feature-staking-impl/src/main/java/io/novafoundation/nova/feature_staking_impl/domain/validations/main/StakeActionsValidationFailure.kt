package io.novafoundation.nova.feature_staking_impl.domain.validations.main

sealed class StakeActionsValidationFailure {

    class UnbondingRequestLimitReached(val limit: Int) : StakeActionsValidationFailure()

    class ControllerRequired(val controllerAddress: String) : StakeActionsValidationFailure()

    class StashRequired(val stashAddress: String) : StakeActionsValidationFailure()
}
