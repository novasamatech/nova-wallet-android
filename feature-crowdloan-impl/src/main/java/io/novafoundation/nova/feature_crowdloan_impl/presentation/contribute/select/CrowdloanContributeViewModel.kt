package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.api.of
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.hasExtraBonusFlow
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.CrowdloanContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidationPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.contributeValidationFailure
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CrowdloanMainFlowFeatures
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.SelectContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.model.CrowdloanDetailsModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.model.LearnMoreModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataFromParcel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private const val DEBOUNCE_DURATION_MILLIS = 500

sealed class ExtraBonusState {

    object NotSupported : ExtraBonusState()

    class Active(val customFlow: String, val payload: BonusPayload, val tokenName: String)

    object Inactive : ExtraBonusState()
}

class CrowdloanContributeViewModel(
    private val assetIconProvider: AssetIconProvider,
    private val router: CrowdloanRouter,
    private val contributionInteractor: CrowdloanContributeInteractor,
    private val resourceManager: ResourceManager,
    assetUseCase: AssetUseCase,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val payload: ContributePayload,
    private val validations: Set<ContributeValidation>,
    private val customContributeManager: CustomContributeManager,
) : BaseViewModel(),
    Validatable by validationExecutor,
    Browserable,
    FeeLoaderMixin by feeLoaderMixin {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val parachainMetadata = payload.parachainMetadata?.let(::mapParachainMetadataFromParcel)

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .inBackground()
        .share()

    val assetModelFlow = assetFlow
        .map { mapAssetToAssetModel(assetIconProvider, it, resourceManager) }
        .inBackground()
        .share()

    private val relevantCustomFlowFactory = payload.parachainMetadata?.customFlow?.let {
        customContributeManager.getFactoryOrNull(it)
    }

    val customizationConfiguration: Flow<Pair<SelectContributeCustomization, SelectContributeCustomization.ViewState>?> = flowOf {
        relevantCustomFlowFactory?.selectContributeCustomization?.let {
            it to it.createViewState(
                features = CrowdloanMainFlowFeatures(
                    coroutineScope = this,
                    browserable = Browserable.Presentation.of(openBrowserEvent)
                ),
                parachainMetadata = parachainMetadata!!
            )
        }
    }
        .inBackground()
        .share()

    private val customizedValidationSystem = flowOf {
        val validations = relevantCustomFlowFactory?.selectContributeCustomization?.modifyValidations(validations)
            ?: validations

        ValidationSystem(CompositeValidation(validations))
    }
        .inBackground()
        .share()

    private val customizationPayloadFlow: Flow<Parcelable?> = customizationConfiguration.flatMapLatest {
        it?.let { (_, viewState) -> viewState.customizationPayloadFlow }
            ?: kotlinx.coroutines.flow.flowOf(null)
    }

    val enteredAmountFlow = MutableStateFlow("")

    private val parsedAmountFlow = enteredAmountFlow.mapNotNull { it.toBigDecimalOrNull() ?: BigDecimal.ZERO }

    private val extraBonusFlow = flow {
        val customFlow = payload.parachainMetadata?.customFlow

        if (
            customFlow != null &&
            customContributeManager.hasExtraBonusFlow(customFlow)
        ) {
            emit(ExtraBonusState.Inactive)

            val source = router.customBonusFlow.map {
                if (it != null) {
                    ExtraBonusState.Active(customFlow, it, parachainMetadata!!.token)
                } else {
                    ExtraBonusState.Inactive
                }
            }

            emitAll(source)
        } else {
            emit(ExtraBonusState.NotSupported)
        }
    }
        .share()

    val bonusDisplayFlow = combine(
        extraBonusFlow,
        parsedAmountFlow
    ) { contributionState, amount ->
        when (contributionState) {
            is ExtraBonusState.Active -> {
                contributionState.payload.bonusText(amount)
            }

            is ExtraBonusState.Inactive -> resourceManager.getString(R.string.crowdloan_empty_bonus_title)

            else -> null
        }
    }
        .inBackground()
        .share()

    val unlockHintFlow = assetFlow.map {
        resourceManager.getString(R.string.crowdloan_unlock_hint, it.token.configuration.symbol)
    }
        .inBackground()
        .share()

    val enteredFiatAmountFlow = assetFlow.combine(parsedAmountFlow) { asset, amount ->
        asset.token.amountToFiat(amount).formatAsCurrency(asset.token.currency)
    }
        .inBackground()
        .asLiveData()

    val title = payload.parachainMetadata?.let {
        "${it.name} (${it.token})"
    } ?: payload.paraId.toString()

    val learnCrowdloanModel = payload.parachainMetadata?.let {
        LearnMoreModel(
            text = resourceManager.getString(R.string.crowdloan_learn_v2_2_0, it.name),
            iconLink = it.iconLink
        )
    }

    val estimatedRewardFlow = parsedAmountFlow.map { amount ->
        payload.parachainMetadata?.let { metadata ->
            val estimatedReward = metadata.rewardRate?.let { amount * it }

            estimatedReward?.formatTokenAmount(metadata.token)
        }
    }.share()

    private val crowdloanFlow = contributionInteractor.crowdloanStateFlow(payload.paraId, parachainMetadata)
        .inBackground()
        .share()

    val crowdloanDetailModelFlow = crowdloanFlow.map { crowdloan ->
        CrowdloanDetailsModel(
            leasePeriod = resourceManager.formatDuration(crowdloan.leasePeriodInMillis),
            leasedUntil = resourceManager.formatDateTime(crowdloan.leasedUntilInMillis)
        )
    }
        .inBackground()
        .share()

    init {
        listenFee()
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun bonusClicked() {
        launch {
            val customContributePayload = CustomContributePayload(
                paraId = payload.paraId,
                parachainMetadata = payload.parachainMetadata!!,
                amount = parsedAmountFlow.first(),
                previousBonusPayload = router.latestCustomBonus
            )

            router.openCustomContribute(customContributePayload)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun listenFee() {
        combine(
            parsedAmountFlow.debounce(DEBOUNCE_DURATION_MILLIS.milliseconds),
            extraBonusFlow,
            customizationPayloadFlow,
            ::Triple
        ).mapLatest { (amount, bonusState, customization) ->
            loadFee(amount, bonusState as? ExtraBonusState.Active, customization)
        }
            .launchIn(viewModelScope)
    }

    private suspend fun loadFee(
        amount: BigDecimal,
        bonusActiveState: ExtraBonusState.Active?,
        customizationPayload: Parcelable?,
    ) {
        feeLoaderMixin.loadFeeSuspending(
            retryScope = viewModelScope,
            feeConstructor = {
                val crowdloan = crowdloanFlow.first()

                contributionInteractor.estimateFee(
                    crowdloan,
                    amount,
                    bonusActiveState?.payload,
                    customizationPayload,
                )
            },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = launch {
        _showNextProgress.value = true

        val contributionAmount = parsedAmountFlow.firstOrNull() ?: return@launch

        val customizationPayload = customizationConfiguration.first()?.let {
            val (_, customViewState) = it

            customViewState.customizationPayloadFlow.first()
        }

        val validationPayload = ContributeValidationPayload(
            crowdloan = crowdloanFlow.first(),
            customizationPayload = customizationPayload,
            fee = feeLoaderMixin.awaitFee(),
            asset = assetFlow.first(),
            bonusPayload = router.latestCustomBonus,
            contributionAmount = contributionAmount
        )

        validationExecutor.requireValid(
            validationSystem = customizedValidationSystem.first(),
            payload = validationPayload,
            validationFailureTransformerCustom = { status, actions ->
                contributeValidationFailure(
                    reason = status.reason,
                    validationFlowActions = actions,
                    resourceManager = resourceManager,
                    onOpenCustomContribute = ::bonusClicked
                )
            },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            _showNextProgress.value = false

            openConfirmScreen(it, customizationPayload)
        }
    }

    private fun openConfirmScreen(
        validationPayload: ContributeValidationPayload,
        customizationPayload: Parcelable?,
    ) = launch {
        val confirmContributePayload = ConfirmContributePayload(
            paraId = payload.paraId,
            fee = mapFeeToParcel(validationPayload.fee),
            amount = validationPayload.contributionAmount,
            estimatedRewardDisplay = estimatedRewardFlow.first(),
            bonusPayload = router.latestCustomBonus,
            metadata = payload.parachainMetadata,
            customizationPayload = customizationPayload
        )

        router.openConfirmContribute(confirmContributePayload)
    }

    fun learnMoreClicked() {
        val parachainLink = parachainMetadata?.website ?: return

        openBrowserEvent.value = Event(parachainLink)
    }
}
