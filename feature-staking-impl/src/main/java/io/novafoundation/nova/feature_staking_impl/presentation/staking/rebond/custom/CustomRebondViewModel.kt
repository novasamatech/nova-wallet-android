package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rebond.RebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.rebondValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private const val DEFAULT_AMOUNT = 1
private const val DEBOUNCE_DURATION_MILLIS = 500

class CustomRebondViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val rebondInteractor: RebondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: RebondValidationSystem,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
) : BaseViewModel(),
    FeeLoaderMixin by feeLoaderMixin,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val assetFlow = accountStakingFlow.flatMapLatest {
        interactor.assetFlow(it.controllerAddress)
    }
        .inBackground()
        .share()

    val assetModelFlow = assetFlow
        .map { mapAssetToAssetModel(it, resourceManager, Asset::unbonding, R.string.staking_unbonding_format) }
        .inBackground()
        .asLiveData()

    val enteredAmountFlow = MutableStateFlow(DEFAULT_AMOUNT.toString())

    private val parsedAmountFlow = enteredAmountFlow.mapNotNull { it.toBigDecimalOrNull() }

    val amountFiatFLow = assetFlow.combine(parsedAmountFlow) { asset, amount ->
        asset.token.fiatAmount(amount).formatAsCurrency()
    }
        .inBackground()
        .asLiveData()

    init {
        listenFee()
    }

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    @OptIn(ExperimentalTime::class)
    private fun listenFee() {
        parsedAmountFlow
            .debounce(DEBOUNCE_DURATION_MILLIS.milliseconds)
            .onEach { loadFee(it) }
            .launchIn(viewModelScope)
    }

    private fun loadFee(amount: BigDecimal) {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { token ->
                val amountInPlanks = token.planksFromAmount(amount)

                rebondInteractor.estimateFee(amountInPlanks)
            },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = feeLoaderMixin.requireFee(this) { fee ->
        launch {
            val payload = RebondValidationPayload(
                fee = fee,
                rebondAmount = parsedAmountFlow.first(),
                controllerAsset = assetFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { rebondValidationFailure(it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer(),
                block = ::openConfirm
            )
        }
    }

    private fun openConfirm(validPayload: RebondValidationPayload) {
        _showNextProgress.value = false

        val confirmPayload = ConfirmRebondPayload(validPayload.rebondAmount)

        router.openConfirmRebond(confirmPayload)
    }
}
