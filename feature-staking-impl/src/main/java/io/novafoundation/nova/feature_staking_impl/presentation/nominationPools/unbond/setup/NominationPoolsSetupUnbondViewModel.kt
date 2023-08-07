package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsSetupUnbondViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsUnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsBondMoreValidationSystem,
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

        val fee = originFeeMixin.awaitFee()

//        val payload = NominationPoolsBondMoreValidationPayload(
//            fee = fee,
//            amount = amountChooserMixin.amount.first(),
//            poolMember = poolMemberFlow.first(),
//            asset = assetFlow.first()
//        )
//
//        validationExecutor.requireValid(
//            validationSystem = validationSystem,
//            payload = payload,
//            validationFailureTransformer = { nominationPoolsBondMoreValidationFailure(it, resourceManager) },
//            progressConsumer = showNextProgress.progressConsumer()
//        ) {
//            showNextProgress.value = false
//
//            openConfirm(payload)
//        }
//    }

//    private fun openConfirm(validationPayload: NominationPoolsBondMoreValidationPayload) {
//        val confirmPayload = NominationPoolsConfirmBondMorePayload(
//            amount = validationPayload.amount,
//            fee = validationPayload.fee
//        )
//
//        router.openConfirmBondMore(confirmPayload)
    }
}
