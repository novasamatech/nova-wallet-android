package io.novafoundation.nova.feature_staking_impl.domain.validations.main

sealed class StakeActionsValidationFailure {

    class ControllerRequired(val controllerAddress: String) : StakeActionsValidationFailure()
}
