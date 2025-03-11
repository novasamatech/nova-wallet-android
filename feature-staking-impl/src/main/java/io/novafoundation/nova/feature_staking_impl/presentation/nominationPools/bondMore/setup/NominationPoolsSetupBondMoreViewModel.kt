package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.NominationPoolsBondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.nominationPoolsBondMoreValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints.NominationPoolsBondMoreHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.ext.fullId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsSetupBondMoreViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsBondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsBondMoreValidationSystem,
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    assetUseCase: AssetUseCase,
    hintsFactory: NominationPoolsBondMoreHintsFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val showNextProgress = MutableStateFlow(false)

    private val assetWithOption = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground()

    private val selectedAsset = assetWithOption.map { it.asset }
        .shareInBackground()

    private val selectedChainAsset = selectedAsset.map { it.token.configuration }
        .distinctUntilChangedBy { it.fullId }
        .shareInBackground()

    val poolMember = poolMemberUseCase.currentPoolMemberFlow()
        .filterNotNull()
        .shareInBackground()

    val originFeeMixin = feeLoaderMixinFactory.createDefault(this, selectedChainAsset)

    private val stakeableAmount = selectedAsset.map(interactor::stakeableAmount)

    private val maxActionProvider = maxActionProviderFactory.createCustom(viewModelScope) {
        selectedChainAsset.providingBalance(stakeableAmount)
            .deductFee(originFeeMixin)
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
            inputSource1 = amountChooserMixin.backPressuredPlanks,
            feeConstructor = { _, amount ->
                interactor.estimateFee(amount)
            }
        )
    }

    private fun maybeGoToNext() = launch {
        showNextProgress.value = true

        val fee = originFeeMixin.awaitFee()

        val payload = NominationPoolsBondMoreValidationPayload(
            fee = fee,
            amount = amountChooserMixin.amount.first(),
            poolMember = poolMember.first(),
            asset = selectedAsset.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions ->
                nominationPoolsBondMoreValidationFailure(status, resourceManager, flowActions, amountChooserMixin::setAmount)
            },
            progressConsumer = showNextProgress.progressConsumer()
        ) { updatedPayload ->
            showNextProgress.value = false

            openConfirm(updatedPayload)
        }
    }

    private fun openConfirm(validationPayload: NominationPoolsBondMoreValidationPayload) {
        val confirmPayload = NominationPoolsConfirmBondMorePayload(
            amount = validationPayload.amount,
            fee = mapFeeToParcel(validationPayload.fee)
        )

        router.openConfirmBondMore(confirmPayload)
    }
}
