package io.novafoundation.nova.feature_staking_impl.presentation.common

import android.util.Log
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import kotlinx.coroutines.flow.MutableStateFlow

sealed class SetupStakingProcess {

    object Initial : SetupStakingProcess() {

        fun changeValidatorsFlow() = ChoosingValidators
    }

    object ChoosingValidators : SetupStakingProcess() {

        fun previous() = Initial

        fun next(validators: List<Validator>, selectionMethod: ReadyToSubmit.SelectionMethod): SetupStakingProcess {
            return ReadyToSubmit(validators, selectionMethod)
        }
    }

    class ReadyToSubmit(
        val validators: List<Validator>,
        val selectionMethod: SelectionMethod
    ) : SetupStakingProcess() {

        enum class SelectionMethod {
            RECOMMENDED, CUSTOM
        }

        fun changeValidators(
            newValidators: List<Validator>,
            selectionMethod: SelectionMethod
        ) = ReadyToSubmit(newValidators, selectionMethod)

        fun previous(): ChoosingValidators {
            return ChoosingValidators
        }

        fun finish() = Initial
    }
}

class SetupStakingSharedState {

    val setupStakingProcess = MutableStateFlow<SetupStakingProcess>(SetupStakingProcess.Initial)

    fun set(newState: SetupStakingProcess) {
        Log.d("RX", "${setupStakingProcess.value.javaClass.simpleName} -> ${newState.javaClass.simpleName}")

        setupStakingProcess.value = newState
    }

    inline fun <reified T : SetupStakingProcess> get(): T = setupStakingProcess.value as T

    fun mutate(mutation: (SetupStakingProcess) -> SetupStakingProcess) {
        set(mutation(get()))
    }
}
