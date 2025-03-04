package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.stakeablePlanks
import io.novafoundation.nova.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.bondMoreValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectBondMoreViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val bondMoreInteractor: BondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: BondMoreValidationSystem,
    private val payload: SelectBondMorePayload,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    hintsMixinFactory: ResourcesHintsMixinFactory,
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val assetFlow = accountStakingFlow
        .flatMapLatest { interactor.assetFlow(it.stashAddress) }
        .inBackground()
        .share()

    private val selectedChainAsset = assetFlow.map { it.token.configuration }
        .shareInBackground()

    val originFeeMixin = feeLoaderMixinFactory.createDefault(
        this,
        selectedChainAsset
    )

    private val maxActionProvider = maxActionProviderFactory.createCustom(viewModelScope) {
        assetFlow.providingMaxOf(Asset::stakeablePlanks)
            .deductFee(originFeeMixin)
    }

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = maxActionProvider
    )

    val hintsMixin = hintsMixinFactory.create(
        coroutineScope = this,
        hintsRes = listOf(R.string.staking_hint_reward_bond_more_v2_2_0)
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
            inputSource1 = amountChooserMixin.backPressuredPlanks,
            feeConstructor = { _, amount ->
                bondMoreInteractor.estimateFee(amount, accountStakingFlow.first())
            }
        )
    }

    private fun maybeGoToNext() = launch {
        _showNextProgress.value = true

        val payload = BondMoreValidationPayload(
            stashAddress = stashAddress(),
            fee = originFeeMixin.awaitFee(),
            amount = amountChooserMixin.amount.first(),
            stashAsset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { bondMoreValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            _showNextProgress.value = false

            openConfirm(payload)
        }
    }

    private fun openConfirm(validationPayload: BondMoreValidationPayload) {
        val confirmPayload = ConfirmBondMorePayload(
            amount = validationPayload.amount,
            fee = mapFeeToParcel(validationPayload.fee),
            stashAddress = validationPayload.stashAddress,
        )

        router.openConfirmBondMore(confirmPayload)
    }

    private suspend fun stashAddress() = accountStakingFlow.first().stashAddress
}
