package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.unbondPayloadAutoFix
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.unbondValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmUnbondViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val unbondInteractor: UnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val iconGenerator: AddressIconGenerator,
    private val validationSystem: UnbondValidationSystem,
    private val externalActions: ExternalActions.Presentation,
    private val payload: ConfirmUnbondPayload,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val assetFlow = accountStakingFlow.flatMapLatest {
        interactor.assetFlow(it.controllerAddress)
    }
        .inBackground()
        .share()

    val assetModelFlow = assetFlow
        .map { mapAssetToAssetModel(it, resourceManager, Asset::bonded, R.string.staking_bonded_format) }
        .inBackground()
        .asLiveData()

    val amountFiatFLow = assetFlow.map { asset ->
        asset.token.fiatAmount(payload.amount).formatAsCurrency()
    }
        .inBackground()
        .asLiveData()

    val amount = payload.amount.toString()

    val feeStatusLiveData = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(payload.fee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .inBackground()
        .asLiveData()

    val originAddressModelLiveData = accountStakingFlow.map {
        val address = it.controllerAddress
        val account = interactor.getProjectedAccount(address)

        val addressModel = iconGenerator.createAddressModel(address, AddressIconGenerator.SIZE_SMALL, account.name)

        addressModel
    }
        .inBackground()
        .asLiveData()

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() {
        val originAddressModel = originAddressModelLiveData.value ?: return

        launch {
            externalActions.showExternalActions(ExternalActions.Type.Address(originAddressModel.address), selectedAssetState.chain())
        }
    }

    private fun maybeGoToNext() = launch {
        val asset = assetFlow.first()

        val payload = UnbondValidationPayload(
            asset = asset,
            stash = accountStakingFlow.first(),
            fee = payload.fee,
            amount = payload.amount,
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { unbondValidationFailure(it, resourceManager) },
            autoFixPayload = ::unbondPayloadAutoFix,
            progressConsumer = _showNextProgress.progressConsumer()
        ) { validPayload ->
            sendTransaction(validPayload)
        }
    }

    private fun sendTransaction(validPayload: UnbondValidationPayload) = launch {
        val amountInPlanks = validPayload.asset.token.configuration.planksFromAmount(payload.amount)

        val result = unbondInteractor.unbond(validPayload.stash, validPayload.asset.bondedInPlanks, amountInPlanks)

        _showNextProgress.value = false

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            router.returnToMain()
        } else {
            showError(result.requireException())
        }
    }
}
