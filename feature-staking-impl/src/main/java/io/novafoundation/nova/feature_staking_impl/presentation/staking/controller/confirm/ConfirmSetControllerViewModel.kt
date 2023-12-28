package io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.set.bondSetControllerValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmSetControllerViewModel(
    private val router: StakingRouter,
    private val controllerInteractor: ControllerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val payload: ConfirmSetControllerPayload,
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: SetControllerValidationSystem,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val assetFlow = interactor.assetFlow(payload.stashAddress)
        .inBackground()
        .share()

    val feeStatusFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee.genericFee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .shareInBackground()

    val stashAddressFlow = flowOf {
        generateIcon(payload.stashAddress)
    }.shareInBackground()

    val controllerAddressLiveData = flowOf {
        generateIcon(payload.controllerAddress)
    }.shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val submittingInProgress = MutableStateFlow(false)

    fun confirmClicked() {
        maybeConfirm()
    }

    fun stashClicked() {
        showExternalActions(payload.stashAddress)
    }

    fun controllerClicked() {
        showExternalActions(payload.controllerAddress)
    }

    fun back() {
        router.back()
    }

    private fun showExternalActions(address: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    private fun maybeConfirm() = launch {
        val payload = SetControllerValidationPayload(
            stashAddress = payload.stashAddress,
            controllerAddress = payload.controllerAddress,
            fee = decimalFee,
            transferable = payload.transferable
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            progressConsumer = submittingInProgress.progressConsumer(),
            validationFailureTransformer = { bondSetControllerValidationFailure(it, resourceManager) }
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() = launch {
        val result = controllerInteractor.setController(
            stashAccountAddress = payload.stashAddress,
            controllerAccountAddress = payload.controllerAddress
        )

        submittingInProgress.value = false

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.staking_controller_change_success))

            router.returnToStakingMain()
        }
    }

    private suspend fun generateIcon(address: String) = addressIconGenerator.createAccountAddressModel(
        chain = selectedAssetState.chain(),
        address = address,
    )
}
