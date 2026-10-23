package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.title
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.UnstakeMode
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitExceedsStakeException
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.runtime.ext.accountIdOf
import kotlinx.coroutines.flow.distinctUntilChanged
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMaxAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorExitValidationFailureUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

private data class ExitPreparationRequest(
    val position: BittensorExitPosition?,
    val input: ExitAmountInput,
    val slippageFraction: BigDecimal
)

private data class ExitAmountRequest(val preparation: ExitPreparationRequest, val exit: BittensorExitRequest, val estimateOnly: Boolean)

/** Warnings the user accepted for the request being validated; any change of the request drops them. */
private data class ExitConsent(val acceptedImpactQuote: BittensorExitQuote? = null, val remainderSweepAccepted: Boolean = false)

class BittensorExitAmountViewModel(
    private val analytics: AnalyticsService,
    private val payload: BittensorExitPositionPayload,
    private val bittensorExitInteractor: BittensorExitInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    private val externalActions: ExternalActions.Presentation,
    estimatesMixin: BittensorEstimatesMixin,
    private val retryController: BittensorRetryController,
    private val validationExecutor: ValidationExecutor,
    private val exitValidationSystem: BittensorExitValidationSystem,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val exitFormatter: BittensorExitFormatter,
    private val slippageSettings: BittensorSlippageSettings,
    private val costBasisFormatter: BittensorSaleCostBasisFormatter,
    precisionValidator: BittensorExitPrecisionValidator,
    validatorInteractor: BittensorValidatorInteractor
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val isRoot = payload.netuid().isRoot()

    private companion object {

        const val AMOUNT_DEBOUNCE_MILLIS = 350L
    }

    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.UNSTAKE_SETUP)

    val retryEvents = retryController.events

    private val validating = MutableStateFlow(false)

    private val input = MutableStateFlow(ExitAmountInput(""))

    private var continuationJob: Job? = null

    private val selectedSlippage = if (isRoot) {
        flowOf { bittensorExitInteractor.slippage.fraction }
    } else {
        slippageSettings.observe()
    }.shareInBackground()

    val amount = input.map { it.text }
        .shareInBackground()

    private val positionState = retryController.observe(
        step = "exit position",
        analytics = chainRpcReads,
        transform = { it.withSafeLoading() }
    ) {
        bittensorExitInteractor.observe(payload.metaId, payload.netuid(), payload.hotkeyAddress)
    }
        .shareInBackground()

    val position = positionState.map { it.dataOrNull }
        .shareInBackground()

    private val amountValidator = precisionValidator.forPrecision(position.map { it?.amountPrecision })

    // The input whose amount exceeded the withdrawable stake; shown as a field error instead of a silent disable.
    private val exceedingInput = MutableStateFlow<ExitAmountInput?>(null)

    val amountError = combine(amountValidator.observe(amount), exceedingInput, input) { precision, exceeding, current ->
        if (precision is FieldValidationResult.Error || exceeding != current) {
            precision
        } else {
            FieldValidationResult.Error(BittensorExitValidationFailure.INSUFFICIENT_STAKE.message(resources))
        }
    }
        .shareInBackground()

    private val requestState = combine(
        position,
        input,
        selectedSlippage
    ) { position, input, slippage -> ExitPreparationRequest(position, input, slippage) }
        .flatMapLatest { request ->
            retryController.observe(
                step = "exit quote",
                analytics = chainRpcReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf {
                    delay(AMOUNT_DEBOUNCE_MILLIS)
                    prepareRequest(request)
                }
            }
        }
        .shareInBackground()

    private val request = requestState.map { it.dataOrNull?.exit }
        .shareInBackground()

    private val feeContext = request.mapNotNull { it?.position?.asset?.token?.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorExitRequest>>(
        scope = this,
        feeContext = feeContext
    )

    val feeStatus = combine(
        request,
        feeLoaderMixin.fee
    ) { request, status -> status.forRequest(request) }
        .shareInBackground()

    private val prepared = combine(
        requestState,
        feeStatus
    ) { state, status ->
        val request = state.dataOrNull?.takeUnless { it.estimateOnly }?.exit
        val estimate = status.loadedFeeOrNull()
        when {
            request == null -> state.map { null }
            status is FeeStatus.Error || status is FeeStatus.NoFee -> ExtendedLoadingState.Loaded(null)
            estimate == null || estimate.request != request -> ExtendedLoadingState.Loading
            else -> {
                val fromStake = bittensorExitInteractor.feeFromStake(request.position, estimate)
                val draft = bittensorExitInteractor.prepare(
                    request.position,
                    request.quote,
                    estimate,
                    fromStake
                )
                ExtendedLoadingState.Loaded(draft)
            }
        }
    }
        .shareInBackground()

    val title = position.map {
        if (isRoot) {
            resources.getString(R.string.bittensor_exit_root_title)
        } else {
            resources.getString(
                R.string.bittensor_exit_sell_title,
                it?.subnet?.displayName(resources) ?: resources.getString(R.string.bittensor_subnet_number, payload.netuid)
            )
        }
    }
        .shareInBackground()

    val amountModel = position.map { position ->
        position?.let {
            val icon = if (isRoot) {
                assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration)
            } else {
                it.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon
            }
            val symbol = if (isRoot) {
                it.asset.token.configuration.symbol.value
            } else {
                it.subnet?.name?.ifBlank { it.subnet?.symbol.orEmpty() } ?: resources.getString(R.string.bittensor_subnet_number, payload.netuid)
            }
            ChooseAmountModel(ChooseAmountInputModel(symbol, icon))
        }
    }
        .shareInBackground()

    private val maximumAmount = combine(position, selectedSlippage) { position, slippage ->
        position?.let {
            // Root shows one validator and "Staked: X · Unstake all" for it: with several validators, Unstake all fills
            // that validator's stake (a single-validator exit that can pay its fee from the stake), not the total.
            val maximum = if (isRoot) {
                bittensorExitInteractor.selectedValidatorMaximumAmount(it, slippage)
            } else {
                bittensorExitInteractor.maximumAmount(it, slippage)
            }.coerceAtLeast(BigInteger.ZERO)
            val symbol = if (isRoot) it.asset.token.configuration.symbol else TokenSymbol(it.subnet?.symbol?.ifBlank { "—" } ?: "—")
            BittensorMaxAmount(BigDecimal(maximum, it.amountPrecision), symbol)
        }
    }
        .shareInBackground()

    // Root: the Max of the validator the unstake is for, so it always passes validation.
    val maxAmount = maximumAmount.map { maximum ->
        maximum?.let { tokenFormatter.formatToken(it.amount, it.symbol).toString().ltrIsolated() }
    }
        .shareInBackground()

    val fiatAmount = combine(
        position,
        amount
    ) { position, text ->
        position ?: return@combine null
        // iOS shows the fiat of an empty field as zero.
        val input = text.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val tao = if (isRoot) input else position.subnet?.priceInRao?.let { input * position.asset.token.amountFromPlanks(it) } ?: return@combine null
        formatTaoFiat(position.asset.token, tao)
    }
        .shareInBackground()

    private val saleCostBasis = estimatesMixin.observePositionCostBasis(
        position.map {
            it?.takeUnless { it.isRoot }
                ?.let { BittensorEstimateContext(it.metaId, it.chain) }
        }.distinctUntilChanged()
    )
        .map { resolveSaleCostBasis(it, payload.netuid) }
        .shareInBackground()

    val averagePrice = combine(
        saleCostBasis,
        position
    ) { state, position ->
        state.map { sale ->
            costBasisFormatter.averagePrice(sale, position?.asset?.token, position?.subnet?.symbol)
        }
    }
        .shareInBackground()

    val averagePriceDescription = position.map { position ->
        position?.subnet?.let { resources.getString(R.string.bittensor_average_price_description, it.symbol, it.title(resources)) }
    }
        .shareInBackground()

    val earned = combine(
        saleCostBasis,
        requestState
    ) { state, request ->
        if (request.dataOrNull?.estimateOnly == true) return@combine ExtendedLoadingState.Loaded(costBasisFormatter.profit(null, null))
        bittensorLoadingValue(state, request) {
            val exit = request.dataOrNull?.exit
            costBasisFormatter.profit(state.dataOrNull, exit?.quote, exit?.position?.asset?.token)
        }
    }
        .shareInBackground()

    // Same directory name as the position, review and result screens; the address only as a fallback.
    val validator = flowOf {
        val hotkey = AccountIdKey(validatorInteractor.chain().accountIdOf(payload.hotkeyAddress))
        validatorInteractor.validatorName(payload.netuid(), hotkey)
    }
        .withSafeLoading()
        .map { it.dataOrNull ?: payload.hotkeyAddress.ellipsizeAddress() }
        .shareInBackground()

    val validatorIcon = position.map { position ->
        position?.let { addressIconGenerator.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT) }
    }
        .shareInBackground()

    val received = prepared.mapLoading {
        it?.let { draft ->
            val token = draft.position.asset.token
            val received = token.amountFromPlanks(draft.netReceive)
            AmountModel("≈ " + tokenFormatter.formatToken(received, token.configuration.symbol), formatTaoFiat(token, received))
        } ?: AmountModel("—", null)
    }
        .shareInBackground()

    val rate = requestState.mapLoading { state -> state?.exit?.let { exitFormatter.exchangeRate(it.position, it.quote) } ?: "—" }
        .shareInBackground()

    val swapRateDescription = request.map { request ->
        if (isRoot || request == null) return@map null
        val percentage = request.quote.serviceFeeRate?.movePointRight(2)?.toStripTrailingZerosString() ?: return@map null
        resources.getString(
            R.string.bittensor_swap_rate_sell_description,
            request.position.asset.token.configuration.symbol.value,
            request.position.subnet?.title(resources) ?: resources.getString(R.string.bittensor_subnet_number, payload.netuid),
            percentage
        )
    }
        .shareInBackground()

    val fee = combine(positionState, requestState, feeStatus) { position, quote, status ->
        val estimate = status.loadedFeeOrNull()
        val token = position.dataOrNull?.asset?.token
        when {
            position.isLoading() || quote.isLoading() || status is FeeStatus.Loading -> ExtendedLoadingState.Loading
            else -> ExtendedLoadingState.Loaded(
                estimate?.let {
                    AmountModel(tokenFormatter.formatToken(it.decimalAmount, it.asset.symbol), token?.let { token -> formatTaoFiat(token, it.decimalAmount) })
                } ?: AmountModel("—", null)
            )
        }
    }
        .shareInBackground()

    val feeCaption = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val fromStake = draft.feeFromStake
        resources.getString(
            if (fromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
            draft.position.asset.token.configuration.symbol.value
        )
    }
        .shareInBackground()

    val saleFeeWarning = combine(request, feeStatus) { request, status ->
        if (isRoot || request == null) return@combine null
        val estimate = status.loadedFeeOrNull()?.takeIf { it.request == request } ?: return@combine null
        val fee = tokenFormatter.formatToken(estimate.decimalAmount, estimate.asset.symbol, TokenConfig(roundingMode = RoundingMode.UP))
        val quote = request.quote
        val fromSale = bittensorExitInteractor.feeFromStake(request.position, estimate) && quote.plan?.isBatched != true && quote.exitHotkeys.size <= 1
        resources.getString(if (fromSale) R.string.bittensor_sell_fee_from_sale else R.string.bittensor_sell_fee_from_free, fee)
    }
        .shareInBackground()

    val continueButtonState = combine(
        positionState,
        prepared,
        validating,
        input
    ) { position, prepared, validating, input ->
        bittensorExitButtonState(
            validating = validating,
            input = input.text,
            loading = position.isLoading() || prepared.isLoading(),
            ready = prepared.dataOrNull != null,
            continueLabel = resources.getString(R.string.common_continue),
            enterAmountLabel = resources.getString(R.string.common_enter_amount)
        )
    }
        .shareInBackground()

    init {
        maximumAmount
            .onEach { maximum -> input.update { it.updateMax(maximum?.amount) } }
            .launchIn(this)

        feeLoaderMixin.connectWith(request) { _, request ->
            request?.let {
                val fee = bittensorExitInteractor.estimateFee(it.position, it.quote)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    fun amountChanged(value: String) {
        input.update { it.edit(value) }
    }

    fun maxClicked() = launchUnit {
        val maximum = maximumAmount.first() ?: return@launchUnit
        input.update { it.selectMax(maximum.amount) }
    }

    fun continueClicked(): Unit = continueWith(ExitConsent())

    private fun continueWith(consent: ExitConsent): Unit = launchUnit {
        if (validating.value || continueButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
        val draft = prepared.first().dataOrNull ?: return@launchUnit
        val quoted = requestState.first().dataOrNull ?: return@launchUnit
        if (quoted.exit != BittensorExitRequest(draft.position, draft.quote) || !isCurrent(quoted)) return@launchUnit
        val current = quoted.preparation.input
        val currentAmount = runCatching {
            current.text.toBigDecimalOrNull()
                ?.movePointRight(draft.position.amountPrecision)
                ?.toBigIntegerExact()
        }
            .getOrNull()
        if (currentAmount != draft.quote.amount) return@launchUnit

        validating.value = true
        try {
            val freshPayload = bittensorExitInteractor.validationPayload(draft.position, draft.quote, draft.fee)
            val validationPayload = freshPayload.copy(
                priceImpactAccepted = consent.acceptedImpactQuote != null && freshPayload.latestQuote == consent.acceptedImpactQuote,
                remainderSweepAccepted = consent.remainderSweepAccepted
            )
            val carriedCostBasis = saleCostBasis.first().dataOrNull?.carriedPayload()
            validationExecutor.requireValid(
                validationSystem = exitValidationSystem,
                payload = validationPayload,
                validationFailureTransformerCustom = { failure, _ ->
                    bittensorExitValidationFailureUi(
                        failure.reason,
                        resources,
                        isRoot = isRoot,
                        dustRemainder = exitFormatter.dustRemainder(validationPayload, draft.position),
                        onOutcome = { outcome ->
                            failure.reason.analyticsKind()?.let { kind ->
                                analytics.track(
                                    BittensorAnalyticsEvent.ValidationShown(
                                        StakingFlow.UNSTAKE,
                                        kind,
                                        StakingValidationStage.SETUP,
                                        outcome,
                                        if (isRoot) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET
                                    )
                                )
                            }
                        },
                        unstakeAll = { exitAll(current) },
                        acceptRemainder = { withCurrentRequest(quoted) { continueWith(consent.copy(remainderSweepAccepted = true)) } },
                        refreshQuote = { withCurrentRequest(quoted) { input.value = current.copy(revision = current.revision + 1) } },
                        acceptImpact = { withCurrentRequest(quoted) { continueWith(consent.copy(acceptedImpactQuote = validationPayload.latestQuote)) } }
                    )
                }
            ) {
                withCurrentRequest(quoted) {
                    analytics.track(
                        BittensorAnalyticsEvent.UnstakeInitiated(
                            BittensorAnalyticsEvent.NETWORK,
                            if (isRoot) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET,
                            if (draft.quote.fullExit) UnstakeMode.FULL else UnstakeMode.PARTIAL,
                            draft.quote.groupHotkeys.size.takeIf { it > 0 } ?: 1,
                            draft.position.preflight.availability.locked.signum() > 0,
                            draft.quote.slippageFraction.takeUnless { isRoot }?.let { SlippageBucket.from(it.movePointRight(2)) },
                            null
                        )
                    )
                    router.openBittensorExitReview(
                        draft.quote.toPayload(payload).copy(
                            remainderSweepAccepted = validationPayload.remainderSweepAccepted,
                            costBasis = carriedCostBasis
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
        }
    }

    /**
     * "Unstake all" / "Sell all" from the remainder alert: fill Max so the setup re-quotes the full position.
     * Only a later manual edit cancels it; a refreshed position or quote does not (iOS `selectMax`).
     */
    private fun exitAll(validated: ExitAmountInput) = launchUnit {
        validating.first { !it }
        if (validated.hasSameIntent(input.value)) maxClicked()
    }

    fun validatorClicked() = launchUnit {
        val position = position.first() ?: return@launchUnit
        externalActions.showAddressActions(payload.hotkeyAddress, position.chain)
    }

    fun back() = router.back()

    private fun currentSlippage(): BigDecimal = if (isRoot) bittensorExitInteractor.slippage.fraction else slippageSettings.get()

    private suspend fun isCurrent(quoted: ExitAmountRequest): Boolean {
        val current = ExitPreparationRequest(position.first(), input.value, currentSlippage())
        return quoted.preparation == current && requestState.first().dataOrNull == quoted
    }

    private fun withCurrentRequest(quoted: ExitAmountRequest, action: () -> Unit) {
        continuationJob?.cancel()
        continuationJob = launch {
            // Validation callbacks can run before the owning validation's finally block.
            validating.first { !it }
            val decision = exitContinuationDecision(
                quoted.preparation.input,
                input.value,
                quoted.preparation.slippageFraction,
                currentSlippage(),
                isCurrent(quoted)
            )
            when (decision) {
                ExitContinuationDecision.CANCEL -> Unit
                ExitContinuationDecision.CONTINUE -> action()
                ExitContinuationDecision.REVALIDATE -> reprepareAndValidate()
            }
        }
    }

    private suspend fun reprepareAndValidate() {
        val refreshed = input.value.copy(revision = input.value.revision + 1)
        val slippage = currentSlippage()
        input.value = refreshed
        val readyDraft = combine(requestState, prepared, input, positionState, selectedSlippage) { quote, draft, current, position, currentSlippage ->
            when {
                !refreshed.hasSameIntent(current) || slippage != currentSlippage -> false
                position is ExtendedLoadingState.Error || quote is ExtendedLoadingState.Error || draft is ExtendedLoadingState.Error -> false
                position.isLoading() || quote.isLoading() || draft.isLoading() -> null
                quote.dataOrNull == null -> false
                else -> {
                    val request = quote.dataOrNull
                    val prepared = draft.dataOrNull
                    val expected = ExitPreparationRequest(position.dataOrNull, current, currentSlippage)
                    when {
                        request == null || request.preparation != expected -> null
                        prepared == null -> false
                        request.exit != BittensorExitRequest(prepared.position, prepared.quote) -> null
                        else -> true
                    }
                }
            }
        }
        val ready = combine(readyDraft, continueButtonState) { draftReady, button ->
            when {
                draftReady == false -> false
                draftReady == true && button is DescriptiveButtonState.Enabled -> true
                else -> null
            }
        }.first { it != null }
        // A changed position/quote must never inherit a warning's price-impact consent.
        if (ready == true && refreshed.hasSameIntent(input.value) && slippage == currentSlippage()) continueWith(ExitConsent())
    }

    private suspend fun prepareRequest(request: ExitPreparationRequest): ExitAmountRequest? {
        val position = request.position ?: return null
        val entered = request.input.text.toBigDecimalOrNull()?.takeIf { it.isPositive }
        val estimateOnly = entered == null
        val maximum = bittensorExitInteractor.maximumAmount(position, request.slippageFraction)
        val value = entered ?: minOf(BigDecimal.ONE, BigDecimal(maximum, position.amountPrecision))
        val amount = runCatching {
            value.movePointRight(position.amountPrecision).toBigIntegerExact()
        }
            .getOrNull() ?: return null
        if (amount <= BigInteger.ZERO || amount.bitLength() > 64) return null
        val fullExit = position.preflight.canExitAll && amount == position.preflight.totalStake
        val quote = if (amount > maximum) {
            null
        } else {
            try {
                bittensorExitInteractor.quote(position, amount, fullExit, request.slippageFraction)
            } catch (e: BittensorExitExceedsStakeException) {
                null
            }
        }
        exceedingInput.value = request.input.takeIf { quote == null && !estimateOnly }
        quote ?: return null
        return ExitAmountRequest(request, BittensorExitRequest(position, quote), estimateOnly)
    }

    private fun formatTaoFiat(token: Token, amount: BigDecimal): CharSequence? {
        return token.toFiatOrNull(amount)?.let { fiatFormatter.formatFiat(it, token.currency) }
    }
}

/** iOS `SubtensorUnstakeSetupViewModelFactory.createAction`: an empty or zero amount asks for one ("Enter amount…"). */
internal fun bittensorExitButtonState(
    validating: Boolean,
    input: String,
    loading: Boolean,
    ready: Boolean,
    continueLabel: String,
    enterAmountLabel: String
): DescriptiveButtonState = when {
    validating -> DescriptiveButtonState.Loading
    input.toBigDecimalOrNull()?.isPositive != true -> DescriptiveButtonState.Disabled(enterAmountLabel)
    loading -> DescriptiveButtonState.Loading
    !ready -> DescriptiveButtonState.Disabled(continueLabel)
    else -> DescriptiveButtonState.Enabled(continueLabel)
}
