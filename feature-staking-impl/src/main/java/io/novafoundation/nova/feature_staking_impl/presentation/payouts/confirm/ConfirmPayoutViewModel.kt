package io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.payout.PayoutInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.MakePayoutPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.PayoutValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.mapPendingPayoutParcelToPayout
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmPayoutViewModel(
    private val interactor: StakingInteractor,
    private val payoutInteractor: PayoutInteractor,
    private val router: StakingRouter,
    private val payload: ConfirmPayoutPayload,
    private val addressModelGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val validationSystem: ValidationSystem<MakePayoutPayload, PayoutValidationFailure>,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    walletUiUseCase: WalletUiUseCase,
    partialRetriableMixinFactory: PartialRetriableMixin.Factory,
) : BaseViewModel(),
    ExternalActions.Presentation by externalActions,
    FeeLoaderMixin by feeLoaderMixin,
    Validatable by validationExecutor {

    private val assetFlow = interactor.currentAssetFlow()
        .share()

    private val stakingStateFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .share()

    private val payouts = payload.payouts.map(::mapPendingPayoutParcelToPayout)

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    val totalRewardFlow = assetFlow.map {
        mapAmountToAmountModel(payload.totalRewardInPlanks, it)
    }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val initiatorAddressModel = stakingStateFlow.map { stakingState ->
        addressModelGenerator.createAccountAddressModel(selectedAssetState.chain(), stakingState.accountAddress)
    }
        .shareInBackground()

    val partialRetriableMixin = partialRetriableMixinFactory.create(scope = this)

    init {
        loadFee()
    }

    fun accountClicked() {
        launch {
            val address = initiatorAddressModel.first().address

            externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
        }
    }

    fun submitClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    private fun sendTransactionIfValid() = launch {
        val asset = assetFlow.first()
        val accountAddress = stakingStateFlow.first().accountAddress
        val amount = asset.token.configuration.amountFromPlanks(payload.totalRewardInPlanks)

        val makePayoutPayload = MakePayoutPayload(
            originAddress = accountAddress,
            fee = feeLoaderMixin.awaitDecimalFee(),
            totalReward = amount,
            asset = asset,
            payouts = payouts
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = makePayoutPayload,
            validationFailureTransformer = ::payloadValidationFailure,
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(makePayoutPayload)
        }
    }

    private fun sendTransaction(payload: MakePayoutPayload) = launch {
        val result = payoutInteractor.makePayouts(payload)

        partialRetriableMixin.handleMultiResult(
            multiResult = result,
            onSuccess = {
                showMessage(resourceManager.getString(R.string.make_payout_transaction_sent))
                router.returnToStakingMain()
            },
            progressConsumer = _showNextProgress.progressConsumer(),
            onRetryCancelled = { router.back() }
        )
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = {
                payoutInteractor.estimatePayoutFee(payouts)
            },
            onRetryCancelled = ::backClicked
        )
    }

    private fun payloadValidationFailure(reason: PayoutValidationFailure): TitleAndMessage {
        val (titleRes, messageRes) = when (reason) {
            PayoutValidationFailure.CannotPayFee -> R.string.common_not_enough_funds_title to R.string.common_not_enough_funds_message
            PayoutValidationFailure.UnprofitablePayout -> R.string.common_confirmation_title to R.string.staking_warning_tiny_payout
        }

        return resourceManager.getString(titleRes) to resourceManager.getString(messageRes)
    }
}
