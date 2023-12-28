package io.novafoundation.nova.feature_staking_impl.presentation.validators.change

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

fun SetupStakingSharedState.reset() = mutate {
    SetupStakingProcess.Initial
}

fun SetupStakingSharedState.retractRecommended() = mutate {
    if (it is SetupStakingProcess.ReadyToSubmit && it.selectionMethod == SelectionMethod.RECOMMENDED) {
        it.previous()
    } else {
        it
    }
}

fun SetupStakingSharedState.setCustomValidators(
    validators: List<Validator>
) = setValidators(validators, SelectionMethod.CUSTOM)

fun SetupStakingSharedState.setRecommendedValidators(
    validators: List<Validator>
) = setValidators(validators, SelectionMethod.RECOMMENDED)

fun SetupStakingSharedState.activeStake(): Balance {
    return when (val state = setupStakingProcess.value) {
        is SetupStakingProcess.ReadyToSubmit -> state.activeStake
        is SetupStakingProcess.ChoosingValidators -> state.activeStake
        SetupStakingProcess.Initial -> throw IllegalArgumentException("Cannot get active stake from $state state")
    }
}

/**
 * Validators that has been selected by user during current flow
 * Does not count currently selected validators
 */
fun SetupStakingSharedState.getNewValidators(): List<Validator> {
    return when (val process = setupStakingProcess.value) {
        is SetupStakingProcess.ReadyToSubmit -> process.newValidators
        SetupStakingProcess.Initial, is SetupStakingProcess.ChoosingValidators -> {
            throw IllegalArgumentException("Cannot get validators from $process state")
        }
    }
}

/**
 * Validators that should be shown for the user as selected
 * It is either its already updated selection during current flow
 * or its currently selected validators (based on on-chain nominations)
 */
fun SetupStakingProcess.getSelectedValidatorsOrNull(): List<Validator>? {
    return when (this) {
        is SetupStakingProcess.ReadyToSubmit -> newValidators
        is SetupStakingProcess.ChoosingValidators -> currentlySelectedValidators
        SetupStakingProcess.Initial -> null
    }
}

fun SetupStakingProcess.getSelectedValidatorsOrEmpty(): List<Validator> {
    return getSelectedValidatorsOrNull().orEmpty()
}

private fun SetupStakingSharedState.setValidators(
    validators: List<Validator>,
    selectionMethod: SelectionMethod
) = mutate {
    when (it) {
        is SetupStakingProcess.ReadyToSubmit -> it.changeValidators(validators, selectionMethod)
        is SetupStakingProcess.ChoosingValidators -> it.next(validators, selectionMethod)
        SetupStakingProcess.Initial -> throw IllegalArgumentException("Cannot set validators from $it state")
    }
}
