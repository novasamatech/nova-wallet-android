package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.nominations

import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.relaychain
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfirmNominationsViewModel(
    private val interactor: StakingInteractor,
    private val router: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val sharedStateSetup: SetupStakingSharedState,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val tokenUseCase: TokenUseCase
) : BaseViewModel() {

    private val currentSetupStakingProcess = sharedStateSetup.get<SetupStakingProcess.ReadyToSubmit>()

    private val validators = currentSetupStakingProcess.validators

    val selectedValidatorsLiveData = liveData(Dispatchers.Default) {
        emit(convertToModels(validators, tokenUseCase.currentToken()))
    }

    val toolbarTitle = selectedValidatorsLiveData.map {
        resourceManager.getString(R.string.staking_selected_validators_mask, it.size)
    }

    fun backClicked() {
        router.back()
    }

    fun validatorInfoClicked(validatorModel: ValidatorModel) {
        viewModelScope.launch {
            val stakeTarget = mapValidatorToValidatorDetailsParcelModel(validatorModel.stakeTarget)
            val payload = StakeTargetDetailsPayload.relaychain(stakeTarget, interactor)

            router.openValidatorDetails(payload)
        }
    }

    private suspend fun convertToModels(
        validators: List<Validator>,
        token: Token,
    ): List<ValidatorModel> {
        val chain = selectedAssetState.chain()

        return validators.map {
            mapValidatorToValidatorModel(chain, it, addressIconGenerator, token)
        }
    }
}
