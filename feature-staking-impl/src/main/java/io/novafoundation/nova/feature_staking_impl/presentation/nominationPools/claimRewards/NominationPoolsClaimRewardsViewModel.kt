package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.NominationPoolsClaimRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations.NominationPoolsClaimRewardsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations.NominationPoolsClaimRewardsValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations.nominationPoolsClaimRewardsValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsClaimRewardsViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsClaimRewardsInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsClaimRewardsValidationSystem,
    private val stakingSharedState: StakingSharedState,
    private val externalActions: ExternalActions.Presentation,
    selectedAccountUseCase: SelectedAccountUseCase,
    walletUiUseCase: WalletUiUseCase,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    assetUseCase: AssetUseCase,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val shouldRestakeInput = MutableStateFlow(false)

    private val pendingRewards = interactor.pendingRewardsFlow()
        .shareInBackground()

    val pendingRewardsAmountModel = combine(pendingRewards.map { it.amount }, assetFlow, ::mapAmountToAmountModel)
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeLoaderMixin = feeLoaderMixinFactory.create(assetFlow)

    val originAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { stakingSharedState.chain() }
        .shareInBackground()


    init {
        listenFee()
    }

    fun confirmClicked() {
        claimRewardsIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = originAddressModelFlow.first().address
        val chain = stakingSharedState.chain()

        externalActions.showAddressActions(address, chain)
    }

    private fun listenFee() {
        feeLoaderMixin.connectWith(
            inputSource = shouldRestakeInput,
            scope = viewModelScope,
            feeConstructor = { shouldRestake -> interactor.estimateFee(shouldRestake) }
        )
    }

    private fun claimRewardsIfValid() = launch {
        _showNextProgress.value = true

        val shouldRestake = shouldRestakeInput.first()
        val pendingRewards = pendingRewards.first()

        val payload = NominationPoolsClaimRewardsValidationPayload(
            fee = feeLoaderMixin.awaitDecimalFee(),
            pendingRewardsPlanks = pendingRewards.amount,
            asset = assetFlow.first(),
            chain = stakingSharedState.chain(),
            poolMember = pendingRewards.poolMember
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { nominationPoolsClaimRewardsValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = { sendTransaction(shouldRestake) }
        )
    }

    private fun sendTransaction(shouldRestake: Boolean) = launch {
        interactor.claimRewards(shouldRestake)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.returnToStakingMain()
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }
}
