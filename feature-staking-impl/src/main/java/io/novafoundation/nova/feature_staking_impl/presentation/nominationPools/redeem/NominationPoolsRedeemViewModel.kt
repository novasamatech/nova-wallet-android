package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem

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
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.NominationPoolsRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations.NominationPoolsRedeemValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations.NominationPoolsRedeemValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations.nominationPoolsRedeemValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class NominationPoolsRedeemViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsRedeemInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsRedeemValidationSystem,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val stakingSharedState: StakingSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    assetUseCase: AssetUseCase,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val poolMemberFlow = poolMemberUseCase.currentPoolMemberFlow()
        .filterNotNull()
        .shareInBackground()

    private val redeemAmount = poolMemberFlow.flatMapLatest { poolMember ->
        interactor.redeemAmountFlow(poolMember, viewModelScope)
    }

    val redeemAmountModel = combine(redeemAmount, assetFlow, ::mapAmountToAmountModel)
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
        redeemIfValid()
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
            inputSource = poolMemberFlow,
            scope = viewModelScope,
            feeConstructor = { interactor.estimateFee(it) }
        )
    }

    private fun redeemIfValid() = launch {
        val asset = assetFlow.first()

        val payload = NominationPoolsRedeemValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            asset = asset,
            chain = stakingSharedState.chain()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { nominationPoolsRedeemValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = { sendTransaction() }
        )
    }

    private fun sendTransaction() = launch {
        interactor.redeem(poolMemberFlow.first())
            .onSuccess { outcome ->
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishFlow(outcome)
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun finishFlow(outcome: RedeemConsequences) {
        if (outcome.willKillStash) {
            router.returnToMain()
        } else {
            router.returnToStakingMain()
        }
    }
}
