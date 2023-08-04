package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup

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
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints.NominationPoolsBondMoreHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NominationPoolsSetupBondMoreViewModel(
    private val router: NominationPoolsRouter,
    private val interactor: NominationPoolsBondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: NominationPoolsBondMoreValidationSystem,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    assetUseCase: AssetUseCase,
    hintsFactory: NominationPoolsBondMoreHintsFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val showNextProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceField = Asset::transferable,
        balanceLabel = R.string.wallet_balance_transferable
    )

    val poolMember = poolMemberUseCase.currentPoolMemberFlow()
        .filterNotNull()
        .shareInBackground()

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
        feeLoaderMixin.connectWith(
            inputSource = amountChooserMixin.backPressuredAmount,
            scope = this,
            feeConstructor = { amount ->
                interactor.estimateFee(amount.toPlanks())
            }
        )
    }

    private fun maybeGoToNext() = launch {
        showNextProgress.value = true

        val fee = feeLoaderMixin.awaitFee()

        val payload = NominationPoolsBondMoreValidationPayload(
            fee = fee,
            amount = amountChooserMixin.amount.first(),
            poolMember = poolMember.first(),
            asset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { nominationPoolsBondMoreValidationFailure(it, resourceManager) },
            progressConsumer = showNextProgress.progressConsumer()
        ) {
            showNextProgress.value = false

            openConfirm(payload)
        }
    }

    private fun openConfirm(validationPayload: NominationPoolsBondMoreValidationPayload) {
        showMessage("Ready to open confirm")
    }
}
