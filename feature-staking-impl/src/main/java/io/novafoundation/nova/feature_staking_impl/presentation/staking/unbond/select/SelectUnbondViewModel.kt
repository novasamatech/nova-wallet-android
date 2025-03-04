package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.stakeablePlanks
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints.UnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.unbondValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableAmountModelOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class SelectUnbondViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val unbondInteractor: UnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: UnbondValidationSystem,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    unbondHintsMixinFactory: UnbondHintsMixinFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .shareInBackground()

    private val assetFlow = accountStakingFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .shareInBackground()

    private val chainAssetFlow = assetFlow.map { it.token.configuration }
        .shareInBackground()

    val transferableFlow = assetFlow.mapLatest(::transferableAmountModelOf)
        .shareInBackground()

    val hintsMixin = unbondHintsMixinFactory.create(coroutineScope = this)

    val originFeeMixin = feeLoaderMixinFactory.createDefault(
        this,
        chainAssetFlow,
        FeeLoaderMixinV2.Configuration(onRetryCancelled = ::backClicked)
    )

    private val maxActionProvider = maxActionProviderFactory.createCustom(viewModelScope) {
        assetFlow.providingMaxOf(Asset::bondedInPlanks)
    }

    val amountMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = maxActionProvider
    )

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
            inputSource1 = amountMixin.backPressuredPlanks,
            feeConstructor = { _, amount ->
                val asset = assetFlow.first()

                unbondInteractor.estimateFee(accountStakingFlow.first(), asset.bondedInPlanks, amount)
            }
        )
    }

    private fun maybeGoToNext() = launch {
        _showNextProgress.value = true

        val asset = assetFlow.first()

        val payload = UnbondValidationPayload(
            stash = accountStakingFlow.first(),
            asset = asset,
            fee = originFeeMixin.awaitFee(),
            amount = amountMixin.amount.first(),
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions -> unbondValidationFailure(status, flowActions, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) { correctPayload ->
            _showNextProgress.value = false

            openConfirm(correctPayload)
        }
    }

    private fun openConfirm(validationPayload: UnbondValidationPayload) {
        val confirmUnbondPayload = ConfirmUnbondPayload(
            amount = validationPayload.amount,
            fee = mapFeeToParcel(validationPayload.fee)
        )

        router.openConfirmUnbond(confirmUnbondPayload)
    }
}
