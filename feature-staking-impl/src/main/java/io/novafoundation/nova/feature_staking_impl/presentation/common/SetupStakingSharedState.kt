package io.novafoundation.nova.feature_staking_impl.presentation.common

import android.util.Log
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.flow.MutableStateFlow

sealed class SetupStakingProcess {

    object Initial : SetupStakingProcess() {

        fun next(
            activeStake: Balance,
            validators: List<Validator>,
            selectionMethod: ReadyToSubmit.SelectionMethod
        ): SetupStakingProcess {
            return ReadyToSubmit(activeStake, validators, selectionMethod)
        }
    }

    class ReadyToSubmit(
        val activeStake: Balance,
        val validators: List<Validator>,
        val selectionMethod: SelectionMethod
    ) : SetupStakingProcess() {

        enum class SelectionMethod {
            RECOMMENDED, CUSTOM
        }

        fun changeValidators(
            newValidators: List<Validator>,
            selectionMethod: SelectionMethod
        ) = ReadyToSubmit(activeStake, newValidators, selectionMethod)

        fun previous(): Initial {
            return Initial
        }

        fun reset() = Initial
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
