package io.novafoundation.nova.feature_staking_impl.presentation.common

import android.util.Log
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.flow.MutableStateFlow

sealed class SetupStakingProcess {

    object Initial : SetupStakingProcess() {

        fun next(
            activeStake: Balance,
            currentlyActiveValidators: List<Validator>,
        ): ChoosingValidators {
            return ChoosingValidators(currentlySelectedValidators = currentlyActiveValidators, activeStake)
        }
    }

    class ChoosingValidators(
        val currentlySelectedValidators: List<Validator>,
        val activeStake: Balance,
    ) : SetupStakingProcess() {

        fun next(
            newValidators: List<Validator>,
            selectionMethod: ReadyToSubmit.SelectionMethod,
        ) = ReadyToSubmit(
            activeStake = activeStake,
            newValidators = newValidators,
            selectionMethod = selectionMethod,
            currentlySelectedValidators = currentlySelectedValidators
        )
    }

    data class ReadyToSubmit(
        val activeStake: Balance,
        val newValidators: List<Validator>,
        val selectionMethod: SelectionMethod,
        val currentlySelectedValidators: List<Validator>,
    ) : SetupStakingProcess() {

        enum class SelectionMethod {
            RECOMMENDED, CUSTOM
        }

        fun changeValidators(
            newValidators: List<Validator>,
            selectionMethod: SelectionMethod
        ) = copy(newValidators = newValidators, selectionMethod = selectionMethod)

        fun previous(): ChoosingValidators {
            return ChoosingValidators(currentlySelectedValidators, activeStake)
        }
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
