package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm

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
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.stakedBalance
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.nominationPoolsUnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsConfirmUnbondViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsUnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsUnbondValidationSystem,
    private val payload: NominationPoolsConfirmUnbondPayload,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val stakingSharedState: StakingSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    assetUseCase: AssetUseCase,
    hintsFactory: NominationPoolsUnbondHintsFactory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    val hintsMixin = hintsFactory.create(coroutineScope = this)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val amountModelFlow = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeStatusFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .shareInBackground()

    val originAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { stakingSharedState.chain() }
        .shareInBackground()

    private val poolMemberStateFlow = interactor.poolMemberStateFlow(viewModelScope)
        .shareInBackground()

    private val poolMemberFlow = poolMemberStateFlow.map { it.poolMember }

    private val stakedBalance = poolMemberStateFlow.map { it.stakedBalance }

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = originAddressModelFlow.first().address
        val chain = stakingSharedState.chain()

        externalActions.showAddressActions(address, chain)
    }

    private fun maybeGoToNext() = launch {
        val asset = assetFlow.first()
        val stakedBalance = asset.token.amountFromPlanks(stakedBalance.first())

        val payload = NominationPoolsUnbondValidationPayload(
            fee = decimalFee,
            amount = payload.amount,
            poolMember = poolMemberFlow.first(),
            asset = asset,
            stakedBalance = stakedBalance,
            sharedComputationScope = viewModelScope,
            chain = stakingSharedState.chain()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions -> nominationPoolsUnbondValidationFailure(status, flowActions, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = ::sendTransaction
        )
    }

    private fun sendTransaction(validationPayload: NominationPoolsUnbondValidationPayload) = launch {
        val token = validationPayload.asset.token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        interactor.unbond(validationPayload.poolMember, amountInPlanks)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { finishFlow() }
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun finishFlow() {
        router.returnToStakingMain()
    }
}
