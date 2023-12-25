package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.setter
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.NominationPoolsBondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.nominationPoolsBondMoreValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints.NominationPoolsBondMoreHintsFactory
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsConfirmBondMoreViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsBondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsBondMoreValidationSystem,
    private val externalActions: ExternalActions.Presentation,
    private val stakingSharedState: StakingSharedState,
    private val payload: NominationPoolsConfirmBondMorePayload,
    poolMemberUseCase: NominationPoolMemberUseCase,
    hintsFactory: NominationPoolsBondMoreHintsFactory,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    val hintsMixin = hintsFactory.create(coroutineScope = this)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val amountFlow = MutableStateFlow(payload.amount)

    val amountModelFlow = combine(amountFlow, assetFlow, ::mapAmountToAmountModel)
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeStatusFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee.networkFee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .shareInBackground()

    val originAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { stakingSharedState.chain() }
        .shareInBackground()

    private val poolMember = poolMemberUseCase.currentPoolMemberFlow()
        .filterNotNull()
        .shareInBackground()

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
        val payload = NominationPoolsBondMoreValidationPayload(
            fee = decimalFee,
            amount = amountFlow.first(),
            poolMember = poolMember.first(),
            asset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions ->
                nominationPoolsBondMoreValidationFailure(status, resourceManager, flowActions, amountFlow.setter())
            },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = ::sendTransaction
        )
    }

    private fun sendTransaction(validationPayload: NominationPoolsBondMoreValidationPayload) = launch {
        val token = validationPayload.asset.token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        interactor.bondMore(amountInPlanks)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishFlow()
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun finishFlow() {
        router.returnToStakingMain()
    }
}
