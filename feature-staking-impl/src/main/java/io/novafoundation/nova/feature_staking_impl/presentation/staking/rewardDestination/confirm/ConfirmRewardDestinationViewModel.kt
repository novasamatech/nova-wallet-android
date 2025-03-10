package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
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
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
    private val validationExecutor: ValidationExecutor,
    private val payload: ConfirmRewardDestinationPayload,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val stashFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .shareInBackground()

    private val controllerAssetFlow = stashFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val originAccountModelFlow = stashFlow.map {
        addressIconGenerator.createAccountAddressModel(it.chain, it.controllerAddress)
    }
        .shareInBackground()

    val rewardDestinationFlow = flowOf {
        mapRewardDestinationParcelModelToRewardDestinationModel(payload.rewardDestination)
    }
        .shareInBackground()

    val feeStatusFlow = controllerAssetFlow.map {
        FeeStatus.Loaded(mapFeeToFeeModel(decimalFee, it.token))
    }
        .shareInBackground()

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val originAddress = originAccountModelFlow.first().address

        showAddressExternalActions(originAddress)
    }

    fun payoutAccountClicked() = launch {
        val payoutDestination = rewardDestinationFlow.first() as? RewardDestinationModel.Payout ?: return@launch

        showAddressExternalActions(payoutDestination.destination.address)
    }

    private suspend fun showAddressExternalActions(address: String) {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    private suspend fun mapRewardDestinationParcelModelToRewardDestinationModel(
        rewardDestinationParcelModel: RewardDestinationParcelModel,
    ): RewardDestinationModel {
        return when (rewardDestinationParcelModel) {
            is RewardDestinationParcelModel.Restake -> RewardDestinationModel.Restake
            is RewardDestinationParcelModel.Payout -> {
                val address = rewardDestinationParcelModel.targetAccountAddress
                val addressModel = addressIconGenerator.createAccountAddressModel(selectedAssetState.chain(), address)

                RewardDestinationModel.Payout(addressModel)
            }
        }
    }

    private fun sendTransactionIfValid() = launch {
        val rewardDestinationModel = rewardDestinationFlow.first()

        val controllerAsset = controllerAssetFlow.first()
        val stashState = stashFlow.first()

        val payload = RewardDestinationValidationPayload(
            availableControllerBalance = controllerAsset.transferable,
            fee = decimalFee,
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

    private fun sendTransaction(
        stashState: StakingState.Stash,
        rewardDestination: RewardDestination,
    ) = launch {
        val setupResult = rewardDestinationInteractor.changeRewardDestination(stashState, rewardDestination)

        _showNextProgress.value = false

        if (setupResult.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            router.returnToStakingMain()
        } else {
            showError(setupResult.requireException())
        }
    }
}
