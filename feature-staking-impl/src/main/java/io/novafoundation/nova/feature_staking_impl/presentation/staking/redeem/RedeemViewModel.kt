package io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.utils.requireValue
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RedeemViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val redeemInteractor: RedeemInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: RedeemValidationSystem,
    private val iconGenerator: AddressIconGenerator,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val payload: RedeemPayload,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .share()

    private val assetFlow = accountStakingFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .share()

    val amountLiveData = assetFlow.map { asset ->
        val redeemable = asset.redeemable

        redeemable.format() to asset.token.fiatAmount(redeemable).formatAsCurrency()
    }
        .inBackground()
        .asLiveData()

    val assetModelLiveData = assetFlow.map { asset ->
        mapAssetToAssetModel(asset, resourceManager, Asset::redeemable, R.string.staking_redeemable_format)
    }

    val originAddressModelLiveData = accountStakingFlow.map {
        val address = it.controllerAddress
        val account = interactor.getProjectedAccount(address)

        iconGenerator.createAddressModel(address, AddressIconGenerator.SIZE_SMALL, account.name)
    }
        .inBackground()
        .asLiveData()

    init {
        loadFee()
    }

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() {
        val address = originAddressModelLiveData.value?.address ?: return

        launch {
            externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
        }
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { redeemInteractor.estimateFee(accountStakingFlow.first()) },
            onRetryCancelled = ::backClicked
        )
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val asset = assetFlow.first()

            val validationPayload = RedeemValidationPayload(
                fee = fee,
                asset = asset
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = validationPayload,
                validationFailureTransformer = { redeemValidationFailure(it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction(it)
            }
        }
    }

    private fun sendTransaction(redeemValidationPayload: RedeemValidationPayload) = launch {
        val result = redeemInteractor.redeem(accountStakingFlow.first(), redeemValidationPayload.asset)

        _showNextProgress.value = false

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            when {
                payload.overrideFinishAction != null -> payload.overrideFinishAction.invoke(router)
                result.requireValue().willKillStash -> router.returnToMain()
                else -> router.returnToStakingBalance()
            }
        } else {
            showError(result.requireException())
        }
    }
}
