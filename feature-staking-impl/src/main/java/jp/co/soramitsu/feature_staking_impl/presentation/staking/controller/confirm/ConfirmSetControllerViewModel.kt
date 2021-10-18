package jp.co.soramitsu.feature_staking_impl.presentation.staking.controller.confirm

import androidx.lifecycle.liveData
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.createAddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.mixin.api.Validatable
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.validation.ValidationExecutor
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_staking_impl.R
import jp.co.soramitsu.feature_staking_impl.domain.StakingInteractor
import jp.co.soramitsu.feature_staking_impl.domain.staking.controller.ControllerInteractor
import jp.co.soramitsu.feature_staking_impl.domain.validations.controller.SetControllerValidationPayload
import jp.co.soramitsu.feature_staking_impl.domain.validations.controller.SetControllerValidationSystem
import jp.co.soramitsu.feature_staking_impl.presentation.StakingRouter
import jp.co.soramitsu.feature_staking_impl.presentation.staking.controller.set.bondSetControllerValidationFailure
import jp.co.soramitsu.feature_wallet_api.data.mappers.mapFeeToFeeModel
import jp.co.soramitsu.feature_wallet_api.presentation.mixin.fee.FeeStatus
import jp.co.soramitsu.runtime.state.SingleAssetSharedState
import jp.co.soramitsu.runtime.state.chain
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
    private val selectedAssetState: SingleAssetSharedState
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val assetFlow = interactor.assetFlow(payload.stashAddress)
        .share()

    val feeStatusLiveData = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(payload.fee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .inBackground()
        .asLiveData()

    val stashAddressLiveData = liveData {
        emit(generateIcon(payload.stashAddress))
    }
    val controllerAddressLiveData = liveData {
        emit(generateIcon(payload.controllerAddress))
    }

    fun confirmClicked() {
        maybeConfirm()
    }

    fun openStashExternalActions() {
        showExternalActions(payload.stashAddress)
    }

    fun openControllerExternalActions() {
        showExternalActions(payload.controllerAddress)
    }

    private fun showExternalActions(address: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    private fun maybeConfirm() = launch {

        val payload = SetControllerValidationPayload(
            stashAddress = payload.stashAddress,
            controllerAddress = payload.controllerAddress,
            fee = payload.fee,
            transferable = payload.transferable
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
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

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.staking_controller_change_success))

            router.returnToMain()
        }
    }

    private suspend fun generateIcon(address: String) = addressIconGenerator
        .createAddressModel(
            address,
            AddressIconGenerator.SIZE_SMALL,
            interactor.getProjectedAccount(address).name
        )

    fun back() {
        router.back()
    }
}
