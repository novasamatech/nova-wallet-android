package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
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
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.bondMoreValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmBondMoreViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val bondMoreInteractor: BondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val iconGenerator: AddressIconGenerator,
    private val validationSystem: BondMoreValidationSystem,
    private val externalActions: ExternalActions.Presentation,
    private val payload: ConfirmBondMorePayload,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val assetFlow = interactor.assetFlow(payload.stashAddress)
        .inBackground()
        .share()

    val assetModelFlow = assetFlow
        .map { mapAssetToAssetModel(it, resourceManager) }
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

    val originAddressModelLiveData = liveData {
        val address = payload.stashAddress
        val account = interactor.getProjectedAccount(address)

        val addressModel = iconGenerator.createAddressModel(address, AddressIconGenerator.SIZE_SMALL, account.name)

        emit(addressModel)
    }

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(payload.stashAddress), selectedAssetState.chain())
    }

    private fun maybeGoToNext() = launch {
        val payload = BondMoreValidationPayload(
            stashAddress = payload.stashAddress,
            fee = payload.fee,
            amount = payload.amount,
            chainAsset = assetFlow.first().token.configuration
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { bondMoreValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() = launch {
        val token = assetFlow.first().token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        val result = bondMoreInteractor.bondMore(payload.stashAddress, amountInPlanks)

        _showNextProgress.value = false

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            finishFlow()
        } else {
            showError(result.requireException())
        }
    }

    private fun finishFlow() = when {
        payload.overrideFinishAction != null -> payload.overrideFinishAction.invoke(router)
        else -> router.returnToStakingBalance()
    }
}
