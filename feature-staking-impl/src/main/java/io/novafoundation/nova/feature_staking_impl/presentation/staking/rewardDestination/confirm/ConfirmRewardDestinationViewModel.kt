package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapRewardDestinationModelToRewardDestination
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.RewardDestinationParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select.rewardDestinationValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmRewardDestinationViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val validationSystem: RewardDestinationValidationSystem,
    private val rewardDestinationInteractor: ChangeRewardDestinationInteractor,
    private val externalActions: ExternalActions.Presentation,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val validationExecutor: ValidationExecutor,
    private val payload: ConfirmRewardDestinationPayload,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val stashFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val controllerAssetFlow = stashFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .inBackground()
        .share()

    val originAccountModelLiveData = stashFlow.map {
        generateDestinationModel(interactor.getProjectedAccount(it.controllerAddress))
    }
        .inBackground()
        .asLiveData()

    val rewardDestinationLiveData = flowOf(payload)
        .map { mapRewardDestinationParcelModelToRewardDestinationModel(it.rewardDestination) }
        .inBackground()
        .asLiveData()

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    val feeLiveData = controllerAssetFlow.map {
        FeeStatus.Loaded(mapFeeToFeeModel(payload.fee, it.token))
    }
        .inBackground()
        .asLiveData()

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() {
        val originAddress = originAccountModelLiveData.value?.address ?: return

        showAddressExternalActions(originAddress)
    }

    fun payoutAccountClicked() {
        val payoutDestination = rewardDestinationLiveData.value as? RewardDestinationModel.Payout ?: return

        showAddressExternalActions(payoutDestination.destination.address)
    }

    private fun showAddressExternalActions(address: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    private suspend fun mapRewardDestinationParcelModelToRewardDestinationModel(
        rewardDestinationParcelModel: RewardDestinationParcelModel,
    ): RewardDestinationModel {
        return when (rewardDestinationParcelModel) {
            is RewardDestinationParcelModel.Restake -> RewardDestinationModel.Restake
            is RewardDestinationParcelModel.Payout -> {
                val address = rewardDestinationParcelModel.targetAccountAddress
                val accountDisplay = addressDisplayUseCase(address)
                val addressModel = addressIconGenerator.createAddressModel(address, AddressIconGenerator.SIZE_SMALL, accountDisplay)

                RewardDestinationModel.Payout(addressModel)
            }
        }
    }

    private fun sendTransactionIfValid() {
        val rewardDestinationModel = rewardDestinationLiveData.value ?: return

        launch {
            val controllerAsset = controllerAssetFlow.first()
            val stashState = stashFlow.first()

            val payload = RewardDestinationValidationPayload(
                availableControllerBalance = controllerAsset.transferable,
                fee = payload.fee,
                stashState = stashState
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { rewardDestinationValidationFailure(resourceManager, it) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction(stashState, mapRewardDestinationModelToRewardDestination(rewardDestinationModel))
            }
        }
    }

    private fun sendTransaction(
        stashState: StakingState.Stash,
        rewardDestination: RewardDestination,
    ) = launch {
        val setupResult = rewardDestinationInteractor.changeRewardDestination(stashState, rewardDestination)

        _showNextProgress.value = false

        if (setupResult.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            router.returnToMain()
        } else {
            showError(setupResult.requireException())
        }
    }

    private suspend fun generateDestinationModel(account: StakingAccount): AddressModel {
        return addressIconGenerator.createAddressModel(account.address, AddressIconGenerator.SIZE_SMALL, account.name)
    }
}
