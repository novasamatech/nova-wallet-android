package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.stakedBalance
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.nominationPoolsUnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsSetupUnbondViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsUnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsUnbondValidationSystem,
    private val stakingSharedState: StakingSharedState,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    assetUseCase: AssetUseCase,
    hintsFactory: NominationPoolsUnbondHintsFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val showNextProgress = MutableStateFlow(false)

    private val assetWithOption = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground()

    private val selectedAsset = assetWithOption.map { it.asset }
        .shareInBackground()

    private val selectedChainAsset = selectedAsset.map { it.token.configuration }
        .shareInBackground()

    val originFeeMixin = feeLoaderMixinFactory.createDefault(
        this,
        amountFormatter,
        selectedChainAsset
    )

    private val poolMemberStateFlow = interactor.poolMemberStateFlow(viewModelScope)
        .shareInBackground()

    private val poolMemberFlow = poolMemberStateFlow.map { it.poolMember }

    private val stakedBalance = poolMemberStateFlow.map { it.stakedBalance }

    val transferableBalance = selectedAsset.map {
        amountFormatter.formatAmountToAmountModel(it.transferable, it)
    }.shareInBackground()

    private val maxActionProvider = maxActionProviderFactory.createCustom(viewModelScope) {
        selectedChainAsset.providingBalance(stakedBalance)
    }

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        maxActionProvider = maxActionProvider
    )

    val hintsMixin = hintsFactory.create(coroutineScope = this)

    val buttonState = combine(showNextProgress, amountChooserMixin.amountInput) { inProgress, amountInput ->
        when {
            inProgress -> DescriptiveButtonState.Loading
            amountInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }.shareInBackground()

    init {
        listenFee()
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private fun listenFee() {
        originFeeMixin.connectWith(
            inputSource1 = poolMemberFlow,
            inputSource2 = amountChooserMixin.backPressuredPlanks,
            feeConstructor = { _, poolMember, amount ->
                interactor.estimateFee(poolMember, amount)
            }
        )
    }

    private fun maybeGoToNext() = launch {
        showNextProgress.value = true

        val asset = selectedAsset.first()
        val stakedBalance = asset.token.amountFromPlanks(stakedBalance.first())

        val payload = NominationPoolsUnbondValidationPayload(
            fee = originFeeMixin.awaitFee(),
            poolMember = poolMemberFlow.first(),
            stakedBalance = stakedBalance,
            asset = asset,
            sharedComputationScope = viewModelScope,
            amount = amountChooserMixin.amount.first(),
            chain = stakingSharedState.chain()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions -> nominationPoolsUnbondValidationFailure(status, flowActions, resourceManager) },
            progressConsumer = showNextProgress.progressConsumer()
        ) { updatedPayload ->
            showNextProgress.value = false

            openConfirm(updatedPayload)
        }
    }

    private fun openConfirm(validationPayload: NominationPoolsUnbondValidationPayload) = launch {
        val confirmPayload = NominationPoolsConfirmUnbondPayload(
            amount = validationPayload.amount,
            fee = mapFeeToParcel(validationPayload.fee)
        )

        router.openConfirmUnbond(confirmPayload)
    }
}
