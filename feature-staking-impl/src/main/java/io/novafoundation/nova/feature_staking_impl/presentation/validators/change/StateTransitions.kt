package io.novafoundation.nova.feature_staking_impl.presentation.validators.change

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState

fun SetupStakingSharedState.retractValidators() = mutate {
    when (it) {
        is SetupStakingProcess.ReadyToSubmit -> it.previous().previous()
        is SetupStakingProcess.ChoosingValidators -> it.previous()
        else -> throw IllegalArgumentException("Cannot retract validators from $it state")
    }
}

fun SetupStakingSharedState.setCustomValidators(
    validators: List<Validator>
) = setValidators(validators, SelectionMethod.CUSTOM)

fun SetupStakingSharedState.setRecommendedValidators(
    validators: List<Validator>
) = setValidators(validators, SelectionMethod.RECOMMENDED)


fun SetupStakingSharedState.getSelectedValidators(): List<Validator> {
    return when (val process = setupStakingProcess.value) {
        is SetupStakingProcess.Validators -> emptyList()
        is SetupStakingProcess.ReadyToSubmit -> process.payload.validators
        else -> throw IllegalArgumentException("Cannot get validators from $process state")
    }
}

private fun SetupStakingSharedState.setValidators(
    validators: List<Validator>,
    selectionMethod: SelectionMethod
) = mutate {
    when (it) {
        is SetupStakingProcess.ChoosingValidators -> it.next(validators, selectionMethod)
        is SetupStakingProcess.ReadyToSubmit -> it.changeValidators(validators, selectionMethod)
        else -> throw IllegalArgumentException("Cannot set validators from $it state")
    }
}
