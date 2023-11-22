package io.novafoundation.nova.feature_staking_impl.presentation.validators.change

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

fun SetupStakingSharedState.reset() = mutate {
    when (it) {
        is SetupStakingProcess.ReadyToSubmit -> it.reset()
        else -> throw IllegalArgumentException("Cannot retract validators from $it state")
    }
}

fun SetupStakingSharedState.setCustomValidators(
    validators: List<Validator>
) = setValidators(validators, SelectionMethod.CUSTOM)

fun SetupStakingSharedState.setRecommendedValidators(
    validators: List<Validator>
) = setValidators(validators, SelectionMethod.RECOMMENDED)

fun SetupStakingSharedState.activeStake(): Balance {
    return when(val state = setupStakingProcess.value) {
        is SetupStakingProcess.ReadyToSubmit -> state.activeStake
        else -> throw IllegalArgumentException("Cannot get active stake from $state state")
    }
}

fun SetupStakingSharedState.getSelectedValidators(): List<Validator> {
    return when (val process = setupStakingProcess.value) {
        is SetupStakingProcess.ReadyToSubmit -> process.validators
        else -> throw IllegalArgumentException("Cannot get validators from $process state")
    }
}

private fun SetupStakingSharedState.setValidators(
    validators: List<Validator>,
    selectionMethod: SelectionMethod
) = mutate {
    when (it) {
        is SetupStakingProcess.ReadyToSubmit -> it.changeValidators(validators, selectionMethod)
        else -> throw IllegalArgumentException("Cannot set validators from $it state")
    }
}
