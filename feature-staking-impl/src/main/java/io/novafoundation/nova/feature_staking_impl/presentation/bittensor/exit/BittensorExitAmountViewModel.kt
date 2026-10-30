package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.alphaSymbol
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.resolvedValidatorNames
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.title
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.SubnetDetailsSource

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitGroup
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitSelection
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorRootHoldFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsResponse
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.labeledAmountSubtitle
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitExceedsStakeException
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
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
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import kotlinx.coroutines.flow.filterNotNull
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMaxAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorExitExceedsAvailable
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorExitValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
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
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import io.novafoundation.nova.common.utils.inBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import io.novasama.substrate_sdk_android.extensions.toHexString

private data class ExitPreparationRequest(
    val position: BittensorExitPosition?,
    val input: ExitAmountInput,
    val slippageFraction: BigDecimal,
    val selection: BittensorExitSelection?
)

/**
 * [requested] is the entered amount; the quote carries the planned one, which may differ.
 * The fee is estimated for the [prepared] request; [exit] carries its latest block re-quote, none after a failed one.
 */
private data class ExitAmountRequest(
    val preparation: ExitPreparationRequest,
    val prepared: BittensorExitRequest,
    val estimateOnly: Boolean,
    val requested: BigInteger,
    val exit: BittensorExitRequest? = prepared
) {

    fun isSameRequest(other: ExitAmountRequest?): Boolean = other != null && preparation == other.preparation && prepared == other.prepared
}

private data class ExitMaximum(val display: BittensorMaxAmount, val fill: BigDecimal)

data class BittensorExitValidatorChoice(val hotkey: AccountIdKey) : Identifiable {

    override val identifier: String = hotkey.value.toHexString()
}

private typealias ChooseExitValidatorPayload = ChooseStakedStakeTargetsBottomSheet.Payload<SelectStakeTargetModel<BittensorExitValidatorChoice>>

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
    private val validatorInteractor: BittensorValidatorInteractor,
    private val rootHoldFormatter: BittensorRootHoldFormatter,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val isRoot = payload.netuid().isRoot()

    private companion object {

        const val AMOUNT_DEBOUNCE_MILLIS = 350L
    }

    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.UNSTAKE_SETUP)

    val retryEvents = retryController.events

    private val validating = MutableStateFlow(false)

    private val input = MutableStateFlow(ExitAmountInput(""))

    // Nothing chosen yet resolves to all validators for a group of several.
    private val selection = MutableStateFlow<BittensorExitSelection?>(null)

    private var continuationJob: Job? = null

    // Block re-quotes and the position refresh pause while the setup is hidden; showing it again re-quotes at once.
    private val shown = MutableStateFlow(true)

    private val shownAgain = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val requotes = if (isRoot) {
        emptyFlow<Unit>()
    } else {
        bittensorExitInteractor.requotes(shown, shownAgain)
            .inBackground()
            .shareIn(this, SharingStarted.Eagerly)
    }

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
        bittensorExitInteractor.observe(payload.metaId, payload.netuid(), payload.hotkeyAddress, shown)
    }
        .shareInBackground()

    val position = positionState.map { it.dataOrNull }
        .shareInBackground()

    private val exitGroup = combine(position, selectedSlippage) { position, slippage ->
        position?.let { bittensorExitInteractor.exitGroup(it, slippage) }
    }
        .shareInBackground()

    private val currentSelection = combine(exitGroup, selection) { group, requested -> group?.resolve(requested) }
        .shareInBackground()

    // A validator tap or Continue asks again after a failed name lookup.
    private val validatorNamesRetries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val validatorNames = exitGroup.map { it?.hotkeys.orEmpty().toSet() }
        .resolvedValidatorNames(validatorNamesRetries) { hotkeys -> validatorInteractor.validatorNames(payload.netuid(), hotkeys) }
        .map { it.orEmpty() }
        .shareInBackground()

    private val amountValidator = precisionValidator.forPrecision(position.map { it?.amountPrecision })

    // The input whose amount exceeds the maximum: Continue stays available and explains the maximum.
    private val exceedingInput = MutableStateFlow<ExitAmountInput?>(null)

    val amountError = amountValidator.observe(amount)
        .shareInBackground()

    private val preparedState = combine(
        position,
        input,
        selectedSlippage,
        selection
    ) { position, input, slippage, selection -> ExitPreparationRequest(position, input, slippage, selection) }
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

    // On every block the shown quote follows the pool; the fee stays with the prepared request.
    private val requestState = preparedState.flatMapLatest { state ->
        state.dataOrNull?.let(::requotedEachBlock) ?: flowOf { state }
    }
        .shareInBackground()

    private val feeRequest = requestState.map { it.dataOrNull?.prepared }
        .distinctUntilChanged()
        .shareInBackground()

    private val feeContext = feeRequest.mapNotNull { it?.position?.asset?.token?.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorExitRequest>>(
        scope = this,
        feeContext = feeContext
    )

    val feeStatus = combine(
        feeRequest,
        feeLoaderMixin.fee
    ) { request, status -> status.forRequest(request) }
        .shareInBackground()

    private val prepared = combine(
        requestState,
        feeStatus
    ) { state, status ->
        val quoted = state.dataOrNull?.takeUnless { it.estimateOnly }
        quoted?.let { status.exitDraft(bittensorExitInteractor, it.prepared, it.exit?.quote) } ?: state.map { null }
    }
        .shareInBackground()

    val title = position.map {
        if (isRoot) {
            resources.getString(R.string.bittensor_exit_root_title)
        } else {
            resources.getString(
                R.string.bittensor_exit_sell_title,
                it?.subnet.displayName(payload.netuid, resources)
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
            val symbol = if (isRoot) it.asset.token.configuration.symbol.value else it.subnet.title(payload.netuid, resources)
            ChooseAmountModel(ChooseAmountInputModel(symbol, icon))
        }
    }
        .shareInBackground()

    private val maximumAmount = combine(position, exitGroup, currentSelection) { position, group, selection ->
        if (position == null || group == null || selection == null) return@combine null
        val maximum = group.maximum(selection).coerceAtLeast(BigInteger.ZERO)
        val fill = group.maximumFill(selection).coerceAtLeast(BigInteger.ZERO)
        ExitMaximum(BittensorMaxAmount(BigDecimal(maximum, position.amountPrecision), alphaSymbol(position)), BigDecimal(fill, position.amountPrecision))
    }
        .shareInBackground()

    // The Max of the selected validators, so it always passes validation.
    val maxAmount = maximumAmount.map { maximum ->
        maximum?.display?.let { tokenFormatter.formatToken(it.amount, it.symbol).toString().ltrIsolated() }
    }
        .shareInBackground()

    val fiatAmount = combine(
        position,
        amount,
        requestState
    ) { position, text, state ->
        position ?: return@combine null
        // iOS shows the fiat of an empty field as zero.
        val input = text.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val tao = if (isRoot) {
            input
        } else {
            val spot = bittensorAlphaSpotPrice(position, state.dataOrNull?.exit?.quote) ?: return@combine null
            input * position.asset.token.amountFromPlanks(spot)
        }
        formatTaoFiat(position.asset.token, tao)
    }
        .shareInBackground()

    private val saleCostBasis = estimatesMixin.observeSaleCostBasis(position.filterNotNull(), payload.netuid)
        .shareInBackground()

    val averagePrice = combine(
        saleCostBasis,
        position
    ) { state, position ->
        state.map { costBasisFormatter.averagePrice(it, position) }
    }
        .shareInBackground()

    val averagePriceDescription = position.map { it?.let(exitFormatter::averagePriceDescription) }
        .shareInBackground()

    val earnedDescription = position.map { it?.let(exitFormatter::earnedDescription) }
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
    val validator = combine(position, currentSelection, validatorNames) { position, selection, names ->
        when (selection) {
            null -> ExtendedLoadingState.Loading
            BittensorExitSelection.All -> ExtendedLoadingState.Loaded(resources.getString(R.string.bittensor_exit_all_validators))
            is BittensorExitSelection.Validator -> ExtendedLoadingState.Loaded(
                names[selection.hotkey]
                    ?: (position?.chain?.addressOf(selection.hotkey.value) ?: payload.hotkeyAddress).ellipsizeAddress()
            )
        }
    }
        .shareInBackground()

    val validatorIcon = currentSelection.map { selection ->
        (selection as? BittensorExitSelection.Validator)?.let {
            addressIconGenerator.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }
        .shareInBackground()

    val isValidatorSelectable = exitGroup.map { (it?.hotkeys?.size ?: 0) > 1 }
        .distinctUntilChanged()
        .shareInBackground()

    val legCapNote = combine(exitGroup, currentSelection) { group, selection ->
        if (group != null && selection != null && group.isLegCapped(selection)) {
            resources.getString(R.string.bittensor_exit_legs_cap, BittensorExitGroup.MAX_LEGS)
        } else {
            null
        }
    }
        .shareInBackground()

    val chooseValidatorAction = actionAwaitableMixinFactory.create<ChooseExitValidatorPayload, ChooseStakedStakeTargetsResponse<BittensorExitValidatorChoice>>()

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

    val swapRateDescription = feeRequest.map { request -> request?.let { exitFormatter.swapRateDescription(it.position, it.quote) } }
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

    val feeCaption = prepared.map { it.dataOrNull?.let(exitFormatter::feeSource) }
        .shareInBackground()

    val saleFeeWarning = combine(feeRequest, feeStatus) { request, status ->
        if (isRoot || request == null) return@combine null
        val estimate = status.loadedFeeOrNull()?.takeIf { it.request == request } ?: return@combine null
        val fee = tokenFormatter.formatToken(estimate.decimalAmount, estimate.asset.symbol, TokenConfig(roundingMode = RoundingMode.UP))
        val quote = request.quote
        val fromSale = bittensorExitInteractor.feeFromStake(request.position, estimate) && quote.plan?.isBatched != true && quote.exitHotkeys.size <= 1
        resources.getString(if (fromSale) R.string.bittensor_sell_fee_from_sale else R.string.bittensor_sell_fee_from_free, fee)
    }
        .shareInBackground()

    private val isFullyLocked = combine(exitGroup, currentSelection) { group, selection ->
        group != null && selection != null && group.maximum(selection).isZero
    }
        .distinctUntilChanged()

    val continueButtonState = combine(
        combine(positionState, prepared, ::Pair),
        validating,
        input,
        isFullyLocked,
        exceedingInput
    ) { (position, prepared), validating, input, fullyLocked, exceeding ->
        val label = resources.getString(R.string.common_continue)
        when {
            validating -> DescriptiveButtonState.Loading
            fullyLocked -> DescriptiveButtonState.Disabled(resources.getString(R.string.bittensor_exit_nothing_available_title))
            input.text.toBigDecimalOrNull()?.isPositive != true -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_enter_amount))
            position.isLoading() || prepared.isLoading() -> DescriptiveButtonState.Loading
            exceeding == input -> DescriptiveButtonState.Enabled(label)
            prepared.dataOrNull == null -> DescriptiveButtonState.Disabled(label)
            else -> DescriptiveButtonState.Enabled(label)
        }
    }
        .shareInBackground()

    init {
        maximumAmount
            .onEach { maximum -> input.update { it.updateMax(maximum?.fill) } }
            .launchIn(this)

        feeLoaderMixin.connectWith(feeRequest) { _, request -> bittensorExitInteractor.feeEstimate(request) }
    }

    fun onShown() {
        if (shown.compareAndSet(expect = false, update = true)) shownAgain.tryEmit(Unit)
    }

    fun onHidden() {
        shown.value = false
    }

    fun amountChanged(value: String) {
        input.update { it.edit(value) }
    }

    fun maxClicked() = launchUnit {
        val maximum = maximumAmount.first() ?: return@launchUnit
        input.update { it.selectMax(maximum.fill) }
    }

    fun continueClicked(): Unit = launchUnit {
        validatorNamesRetries.tryEmit(Unit)
        if (validating.value || continueButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
        if (exceedingInput.value == input.value) return@launchUnit showExceedsMaximum()
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
        if (currentAmount != quoted.requested) return@launchUnit
        val group = bittensorExitInteractor.exitGroup(draft.position, quoted.preparation.slippageFraction)
        val selection = group.resolve(quoted.preparation.selection)

        validating.value = true
        try {
            val validationPayload = bittensorExitInteractor.validationPayload(draft.position, draft.quote, draft.fee)
            val carriedCostBasis = saleCostBasis.first().dataOrNull?.carriedPayload()
            val failureLimits = bittensorExitInteractor.failureLimits(validationPayload, draft.position, selection)
            val failureValues = exitFormatter.failureValues(validationPayload, draft.position, failureLimits)
            validationExecutor.requireValid(
                validationSystem = exitValidationSystem,
                payload = validationPayload,
                validationFailureTransformerCustom = { failure, actions ->
                    bittensorExitValidationFailureUi(
                        failure.reason,
                        actions,
                        resources,
                        isRoot = isRoot,
                        values = failureValues,
                        unstakeAll = { exitAll(current) }.takeIf { group.basis(selection).canExitAll },
                        acceptsRate = true,
                        onOutcome = { analytics.trackExitValidation(failure.reason, StakingValidationStage.SETUP, it, isRoot) }
                    )
                }
            ) { validated ->
                withCurrentRequest(quoted) {
                    val exitAnalytics = BittensorExitAnalyticsValues(draft.position, validated.quote)
                    analytics.track(
                        BittensorAnalyticsEvent.UnstakeInitiated(
                            BittensorAnalyticsEvent.NETWORK,
                            exitAnalytics.target,
                            exitAnalytics.mode,
                            exitAnalytics.hotkeys,
                            exitAnalytics.hasLocked,
                            exitAnalytics.slippage,
                            null
                        )
                    )
                    val reviewedHotkey = validated.quote.plan?.slices?.map { it.hotkey }?.distinct()?.singleOrNull() ?: group.anchor(selection)
                    val reviewedPosition = payload.copy(hotkeyAddress = draft.position.chain.addressOf(reviewedHotkey.value))
                    router.openBittensorExitReview(
                        validated.quote.toPayload(reviewedPosition).copy(
                            costBasis = carriedCostBasis,
                            allValidators = selection == BittensorExitSelection.All
                        )
                    )
                    if (draft.quote.amount != quoted.requested) {
                        input.update { it.edit(BigDecimal(draft.quote.amount, draft.position.amountPrecision).toStripTrailingZerosString()) }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A slow node gets the same Retry / Skip dialog as a failed read, not the raw "<read> timed out" text.
            if (!retryController.reportSlowAction("exit setup", e, ::continueClicked)) showError(e)
        } finally {
            validating.value = false
        }
    }

    /**
     * "Unstake all" from the remainder alert: fill Max so the setup re-quotes the full position.
     * Only a later manual edit cancels it; a refreshed position or quote does not.
     */
    private fun exitAll(validated: ExitAmountInput) = launchUnit {
        validating.first { !it }
        if (validated.hasSameIntent(input.value)) maxClicked()
    }

    private suspend fun showExceedsMaximum() {
        val maximum = maxAmount.first() ?: return
        analytics.trackExitValidation(BittensorExitValidationFailure.INSUFFICIENT_STAKE, StakingValidationStage.SETUP, StakingValidationOutcome.BLOCKED, isRoot)
        val (title, message) = resources.bittensorExitExceedsAvailable(maximum)
        showError(title, message)
    }

    /** Opens the subnet's details to read; selling never changes the subnet, so nothing continues from there. */
    fun subnetClicked() {
        if (isRoot) return
        router.openBittensorSubnetDetails(
            BittensorSubnetDetailsPayload(subnetId = payload.netuid, analyticsSource = SubnetDetailsSource.POSITION, viewOnly = true)
        )
    }

    fun validatorClicked() = launchUnit {
        validatorNamesRetries.tryEmit(Unit)
        val position = position.first() ?: return@launchUnit
        val group = exitGroup.first() ?: return@launchUnit
        val selection = currentSelection.first() ?: return@launchUnit
        if (group.hotkeys.size > 1) {
            when (val response = chooseValidatorAction.awaitAction(createValidatorPickerPayload(position, group, selection))) {
                ChooseStakedStakeTargetsResponse.New -> applySelection(BittensorExitSelection.All)
                is ChooseStakedStakeTargetsResponse.Existing -> validatorPicked(response.target)
            }
        } else {
            val address = position.chain.addressOf(group.anchor(selection).value)
            router.openBittensorValidatorInfo(BittensorValidatorInfoPayload(payload.netuid, address))
        }
    }

    private suspend fun validatorPicked(choice: BittensorExitValidatorChoice) {
        val position = position.first() ?: return
        val group = exitGroup.first() ?: return
        when (choice.hotkey) {
            in group.heldHotkeys -> showRootHold(position, choice.hotkey)
            in group.unsellableHotkeys -> showError(
                resources.getString(R.string.bittensor_exit_locked_title),
                resources.getString(R.string.bittensor_exit_nothing_available)
            )
            else -> applySelection(BittensorExitSelection.Validator(choice.hotkey))
        }
    }

    private suspend fun applySelection(newSelection: BittensorExitSelection) {
        if (newSelection == currentSelection.first()) return
        selection.value = newSelection
        input.update { it.edit("") }
    }

    private suspend fun showRootHold(position: BittensorExitPosition, hotkey: AccountIdKey) {
        val hold = bittensorExitInteractor.rootHold(position, hotkey)?.let(rootHoldFormatter::format)
        showError(resources.getString(R.string.bittensor_root_hold_title), hold?.let { it.description + "\n" + it.timeLeft }.orEmpty())
    }

    private suspend fun createValidatorPickerPayload(
        position: BittensorExitPosition,
        group: BittensorExitGroup,
        selection: BittensorExitSelection
    ): ChooseExitValidatorPayload {
        val names = validatorNames.first()
        val rows = group.positions.map { member ->
            val active = member.hotkey !in group.unsellableHotkeys
            val staked = tokenFormatter.formatToken(BigDecimal(member.alpha, position.amountPrecision), alphaSymbol(position))
            SelectStakeTargetModel(
                addressModel = addressIconGenerator.createAccountAddressModel(
                    position.chain,
                    position.chain.addressOf(member.hotkey.value),
                    names[member.hotkey]
                ),
                subtitle = resources.labeledAmountSubtitle(R.string.staking_main_stake_balance_staked, AmountModel(staked, null), selectionActive = active),
                active = active,
                payload = BittensorExitValidatorChoice(member.hotkey)
            )
        }
        val selectedHotkey = (selection as? BittensorExitSelection.Validator)?.hotkey
        return ChooseStakedStakeTargetsBottomSheet.Payload(
            data = rows,
            selected = rows.firstOrNull { it.payload.hotkey == selectedHotkey },
            titleRes = R.string.bittensor_validator,
            newStakeTargetTitleRes = R.string.bittensor_exit_all_validators.takeIf { group.canSellAll },
            newStakeTargetIconRes = null
        )
    }

    private fun alphaSymbol(position: BittensorExitPosition): TokenSymbol {
        return if (isRoot) position.asset.token.configuration.symbol else TokenSymbol(position.subnet.alphaSymbol(payload.netuid))
    }

    fun back() = router.back()

    private fun currentSlippage(): BigDecimal = if (isRoot) bittensorExitInteractor.slippage.fraction else slippageSettings.get()

    private suspend fun isCurrent(quoted: ExitAmountRequest): Boolean {
        val current = ExitPreparationRequest(position.first(), input.value, currentSlippage(), selection.value)
        return quoted.preparation == current && quoted.isSameRequest(requestState.first().dataOrNull)
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
                    val expected = ExitPreparationRequest(position.dataOrNull, current, currentSlippage, selection.value)
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
        // A changed position/quote must never inherit a warning's consent.
        if (ready == true && refreshed.hasSameIntent(input.value) && slippage == currentSlippage()) continueClicked()
    }

    private suspend fun prepareRequest(request: ExitPreparationRequest): ExitAmountRequest? {
        exceedingInput.value = null
        val position = request.position ?: return null
        val group = bittensorExitInteractor.exitGroup(position, request.slippageFraction)
        val selection = group.resolve(request.selection)
        val entered = request.input.text.toBigDecimalOrNull()?.takeIf { it.isPositive }
        val estimateOnly = entered == null
        val maximum = group.maximum(selection)
        val value = entered ?: minOf(BigDecimal.ONE, BigDecimal(maximum, position.amountPrecision))
        val amount = runCatching {
            value.movePointRight(position.amountPrecision).toBigIntegerExact()
        }
            .getOrNull() ?: return null
        if (amount <= BigInteger.ZERO || amount.bitLength() > 64) return null
        val quote = if (amount > maximum) {
            null
        } else {
            try {
                bittensorExitInteractor.quote(position, amount, selection, request.slippageFraction)
            } catch (e: BittensorExitExceedsStakeException) {
                null
            }
        }
        exceedingInput.value = request.input.takeIf { amount > maximum && !estimateOnly }
        quote ?: return null
        return ExitAmountRequest(request, BittensorExitRequest(position, quote), estimateOnly, amount)
    }

    /** A failed re-quote leaves no quote until the next block brings one. */
    private fun requotedEachBlock(request: ExitAmountRequest): Flow<ExtendedLoadingState<ExitAmountRequest?>> = requotes
        .mapLatest<Unit, ExtendedLoadingState<ExitAmountRequest?>> {
            val position = request.prepared.position
            val quote = bittensorExitInteractor.blockRequote(position, request.prepared.quote)
                .onFailure(chainRpcReads::failed)
                .getOrNull()
            ExtendedLoadingState.Loaded(request.copy(exit = quote?.let { BittensorExitRequest(position, it) }))
        }
        .onStart { emit(ExtendedLoadingState.Loaded(request)) }

    private fun formatTaoFiat(token: Token, amount: BigDecimal): CharSequence? {
        return token.toFiatOrNull(amount)?.let { fiatFormatter.formatFiat(it, token.currency) }
    }
}
