package io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.networkType
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.utils.toAddress
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.payout.PayoutInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.MakePayoutPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.PayoutValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
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
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val validationSystem: ValidationSystem<MakePayoutPayload, PayoutValidationFailure>,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    ExternalActions.Presentation by externalActions,
    FeeLoaderMixin by feeLoaderMixin,
    Validatable by validationExecutor {

    private val assetFlow = interactor.currentAssetFlow()
        .share()

    private val stakingStateFlow = interactor.selectedAccountStakingStateFlow()
        .share()

    private val payouts = payload.payouts.map { Payout(it.validatorInfo.address, it.era, it.amountInPlanks) }

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    val totalRewardDisplay = assetFlow.map {
        val token = it.token
        val totalReward = token.amountFromPlanks(payload.totalRewardInPlanks)
        val inToken = totalReward.formatTokenAmount(token.configuration)
        val inFiat = token.fiatAmount(totalReward).formatAsCurrency()

        inToken to inFiat
    }
        .inBackground()
        .asLiveData()

    val rewardDestinationModel = stakingStateFlow.map { stakingState ->
        require(stakingState is StakingState.Stash)

        val networkType = stakingState.accountAddress.networkType()

        val destinationAddress = when (val rewardDestination = interactor.getRewardDestination(stakingState)) {
            RewardDestination.Restake -> stakingState.accountAddress
            is RewardDestination.Payout -> rewardDestination.targetAccountId.toAddress(networkType)
        }

        val destinationAddressDisplay = addressDisplayUseCase(destinationAddress)

        addressModelGenerator.createAddressModel(destinationAddress, AddressIconGenerator.SIZE_SMALL, destinationAddressDisplay)
    }
        .inBackground()
        .asLiveData()

    val initiatorAddressModel = stakingStateFlow.map { stakingState ->
        val initiatorAddress = stakingState.accountAddress
        val initiatorDisplay = addressDisplayUseCase(initiatorAddress)

        addressModelGenerator.createAddressModel(initiatorAddress, AddressIconGenerator.SIZE_SMALL, initiatorDisplay)
    }
        .inBackground()
        .asLiveData()

    init {
        loadFee()
    }

    fun controllerClicked() {
        maybeShowExternalActions { initiatorAddressModel.value?.address }
    }

    fun submitClicked() {
        sendTransactionIfValid()
    }

    fun rewardDestinationClicked() {
        maybeShowExternalActions { rewardDestinationModel.value?.address }
    }

    fun backClicked() {
        router.back()
    }

    private fun sendTransactionIfValid() = feeLoaderMixin.requireFee(this) { fee ->
        launch {
            val tokenType = assetFlow.first().token.configuration
            val accountAddress = stakingStateFlow.first().accountAddress
            val amount = tokenType.amountFromPlanks(payload.totalRewardInPlanks)

            val payoutStakersPayloads = payouts.map { MakePayoutPayload.PayoutStakersPayload(it.era, it.validatorAddress) }

            val makePayoutPayload = MakePayoutPayload(accountAddress, fee, amount, tokenType, payoutStakersPayloads)

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = makePayoutPayload,
                validationFailureTransformer = ::payloadValidationFailure,
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction(makePayoutPayload)
            }
        }
    }

    private fun sendTransaction(payload: MakePayoutPayload) = launch {
        val result = payoutInteractor.makePayouts(payload)

        _showNextProgress.value = false

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.make_payout_transaction_sent))

            router.returnToMain()
        } else {
            showError(result.requireException())
        }
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            viewModelScope,
            feeConstructor = {
                val address = stakingStateFlow.first().accountAddress

                payoutInteractor.estimatePayoutFee(address, payouts)
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

    private fun maybeShowExternalActions(addressProducer: () -> String?) {
        val address = addressProducer() ?: return

        launch {
            externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
        }
    }
}
