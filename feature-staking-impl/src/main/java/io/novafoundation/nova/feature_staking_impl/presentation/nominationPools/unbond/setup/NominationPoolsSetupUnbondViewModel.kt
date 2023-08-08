package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.nominationPoolsUnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.nominationPoolsUnbondValidationPayloadAutoFix
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsSetupUnbondViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsUnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsUnbondValidationSystem,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    assetUseCase: AssetUseCase,
    hintsFactory: NominationPoolsUnbondHintsFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val showNextProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val originFeeMixin = feeLoaderMixinFactory.create(assetFlow)

    private val poolMemberFlow = poolMemberUseCase.currentPoolMemberFlow()
        .filterNotNull()
        .shareInBackground()

    private val bondedPoolStateFlow = poolMemberFlow.flatMapLatest { poolMember ->
        interactor.bondedPoolStateFlow(poolMember.poolId, computationScope = this)
    }
        .shareInBackground()


    private val stakedBalance = combine(bondedPoolStateFlow, poolMemberFlow) { bondedPool, poolMember ->
        bondedPool.amountOf(poolMember.points)
    }.shareInBackground()

    val transferableBalance = assetFlow.map {
        mapAmountToAmountModel(it.transferable, it)
    }.shareInBackground()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        availableBalanceFlow = stakedBalance,
        balanceLabel = R.string.staking_main_stake_balance_staked
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
            inputSource2 = amountChooserMixin.backPressuredAmount,
            scope = this,
            feeConstructor = { poolMember, amount ->
                interactor.estimateFee(poolMember, amount.toPlanks())
            }
        )
    }

    private fun maybeGoToNext() = launch {
        showNextProgress.value = true

        val asset = assetFlow.first()
        val stakedBalance = asset.token.amountFromPlanks(stakedBalance.first())

        val payload = NominationPoolsUnbondValidationPayload(
            fee = originFeeMixin.awaitFee(),
            poolMember = poolMemberFlow.first(),
            stakedBalance = stakedBalance,
            asset = asset,
            sharedComputationScope = viewModelScope,
            amount = amountChooserMixin.amount.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { nominationPoolsUnbondValidationFailure(it, resourceManager) },
            autoFixPayload = ::nominationPoolsUnbondValidationPayloadAutoFix,
            progressConsumer = showNextProgress.progressConsumer()
        ) { updatedPayload ->
            showNextProgress.value = false

            openConfirm(updatedPayload)
        }
    }

    private fun openConfirm(validationPayload: NominationPoolsUnbondValidationPayload) {
        showMessage("Ready to unbond ${mapAmountToAmountModel(validationPayload.amount, validationPayload.asset).token}")
    }
}
