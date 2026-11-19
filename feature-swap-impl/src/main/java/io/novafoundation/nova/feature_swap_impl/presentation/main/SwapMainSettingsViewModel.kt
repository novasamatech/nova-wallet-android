package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_impl.presentation.main.chart.SwapChartMixinFactory
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsState
import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapSelectionInteractor
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import java.util.UUID
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.feature_swap_impl.presentation.common.analytics.toAnalyticsSource
import io.novafoundation.nova.analytics.SwapStage
import io.novafoundation.nova.analytics.AmountBucket
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.AssetCategoryClassifier
import io.novafoundation.nova.analytics.SwapSource
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.accumulate
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatting.CompoundNumberFormatter
import io.novafoundation.nova.common.utils.formatting.DynamicPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.FixedPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.NumberAbbreviation
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.nullOnStart
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.validation.CompoundFieldValidator
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isErrorWithTag
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.domain.swap.swapRateDescriptionMode
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.SwapRateDescriptionMode
import io.novafoundation.nova.feature_swap_api.domain.model.totalTime
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.mapFromModel
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.BuildConfig
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapDustCheckFee
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.toSwapState
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fee.createForSwap
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.LiquidityFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SwapReceiveAmountAboveEDFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapMaxActionProvider
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapMaxActionProviderFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapOperationPayloadMapper
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapInputMixinPriceImpactFiatFormatterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.invokeMaxClick
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.isMaxAction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.PaymentCurrencySelectionMode
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds
import io.novafoundation.nova.feature_wallet_api.data.repository.UsdRateRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.planksToUsd
import io.novasama.substrate_sdk_android.hash.isPositive

private val QUOTE_LOADING_TIMEOUT = 30.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val swapSettingsState: SwapSettingsState,
    private val selectionInteractor: SwapSelectionInteractor,
    chartMixinFactory: SwapChartMixinFactory,
    private val tokenSelectionCommunicator: SwapTokenSelectionCommunicator,
    private val slippageCommunicator: SlippageSettingsCommunicator,
    private val display: SwapFormDisplay,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val assetUseCase: ArbitraryAssetUseCase,
    payload: SwapSettingsPayload,
    private val validationExecutor: ValidationExecutor,
    private val liquidityFieldValidatorFactory: LiquidityFieldValidatorFactory,
    private val swapReceiveAmountAboveEDFieldValidatorFactory: SwapReceiveAmountAboveEDFieldValidatorFactory,
    private val enoughAmountValidatorFactory: EnoughAmountValidatorFactory,
    private val swapInputMixinPriceImpactFiatFormatterFactory: SwapInputMixinPriceImpactFiatFormatterFactory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val swapRateFormatter: SwapRateFormatter,
    private val swapRouteFormatter: SwapRouteFormatter,
    private val maxActionProviderFactory: SwapMaxActionProviderFactory,
    private val operationMapper: SwapOperationPayloadMapper,
    private val tokenFormatter: TokenFormatter,
    private val priceImpactFormatter: PriceImpactFormatter,
    private val priceImpactThresholds: PriceImpactThresholds,
    private val getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
    private val analyticsService: AnalyticsService,
    private val usdRateRepository: UsdRateRepository,
    swapAmountInputMixinFactory: SwapAmountInputMixinFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    Validatable by validationExecutor {

    private val swapFlowScope = viewModelScope

    private val entryPayload = MutableStateFlow(payload)

    private val swapSettings = swapSettingsState.selectedOption

    val chart = chartMixinFactory.create(
        settings = swapSettings,
        initiallyCollapsed = display == SwapFormDisplay.STANDALONE || payload != SwapSettingsPayload.Restore,
        scope = viewModelScope
    )

    private val feeSnapshot = MutableStateFlow<SwapFeeSnapshot?>(null)

    private val maxFeeResetVersion = MutableStateFlow(0)

    private val active = MutableStateFlow(true)

    private var initializedMetaAccountId: Long? = null

    private var selectionRequestId: String? = null

    private var slippageRequestId: String? = null

    val detailsExpanded = MutableStateFlow(true)

    private val chainAssetIn = swapSettings
        .map { it.assetIn }
        .distinctUntilChanged()
        .shareInBackground()

    private val quotingState = MutableStateFlow<QuotingState>(QuotingState.Default)

    private val assetOutFlow = swapSettings.assetFlowOf(SwapSettings::assetOut)
    private val assetInFlow = swapSettings.assetFlowOf(SwapSettings::assetIn)

    private val priceImpact = quotingState.map { quoteState ->
        when (quoteState) {
            is QuotingState.Error, QuotingState.Loading, QuotingState.Default -> null
            is QuotingState.Loaded -> quoteState.quote.priceImpact
        }
    }

    val swapExecutionTime = quotingState
        .map { it.toExecutionEstimate() }
        .shareInBackground()

    val quoteFailed = quotingState.map {
        it is QuotingState.Error && it.error !is SwapQuoteException
    }

    val quoteProblem = quotingState.map {
        when {
            it !is QuotingState.Error -> null
            it.error is SwapQuoteException.NoRoute -> resourceManager.getString(R.string.swap_error_pair_not_supported)
            it.error is SwapQuoteException.NotEnoughLiquidity -> null // Displayed beside the amount field.
            else -> resourceManager.getString(R.string.swap_quote_failed)
        }
    }

    val priceImpactWarning = priceImpact.map { impact ->
        impact?.takeIf { it >= priceImpactThresholds.highPriceImpact }?.let {
            resourceManager.getString(R.string.high_price_impact_detacted_title, it.formatPercents())
        }
    }

    val priceImpactDetails = priceImpact.map { impact ->
        impact?.let(priceImpactFormatter::format)
    }

    val slippageDetails = swapSettings.map { it.slippage.formatPercents() }

    val receiveBalance = assetOutFlow.map { asset ->
        asset?.let {
            resourceManager.getString(R.string.swap_balance_label, tokenFormatter.formatToken(it.transferableInPlanks, it))
        }
    }

    private val originChainFlow = swapSettings
        .mapNotNull { it.assetIn?.chainId }
        .distinctUntilChanged()
        .map { chainRegistry.getChain(it) }
        .shareInBackground()

    val feeMixin = feeLoaderMixinFactory.createForSwap(
        chainAssetIn = swapSettings.mapNotNull { it.assetIn },
        interactor = swapInteractor,
        configuration = Configuration(
            initialState = Configuration.InitialState(
                paymentCurrencySelectionMode = PaymentCurrencySelectionMode.AUTOMATIC_ONLY
            )
        )
    )

    val swapRouteState = combine(quotingState, feeSnapshot, feeMixin.fee) { quote, snapshot, fee ->
        when {
            quote !is QuotingState.Loaded -> quote.toLoadingState { null }
            fee is FeeStatus.Error -> ExtendedLoadingState.Loaded(null)
            fee !is FeeStatus.Loaded || snapshot?.matchesInputs(quote.settings, quote.quoteArgs.metaAccountId) != true -> ExtendedLoadingState.Loading
            else -> ExtendedLoadingState.Loaded(swapRouteFormatter.formatSwapRoute(snapshot.quote.quote))
        }
    }.shareInBackground()

    val minimumReceived = combine(quotingState, feeSnapshot) { quote, snapshot ->
        snapshot?.takeIf { quote is QuotingState.Loaded && it.matchesInputs(quote.settings, quote.quoteArgs.metaAccountId) }?.let {
            tokenFormatter.formatToken(it.fee.segments.last().netFlow.amountOutMin, it.quote.quote.assetOut)
        }
    }

    private val amountInInputState = MutableStateFlow(InputState.empty())

    private val maxAssetInProvider = createMaxActionProvider()

    val amountInInput = swapAmountInputMixinFactory.create(
        coroutineScope = viewModelScope,
        tokenFlow = assetInFlow.token().nullOnStart(),
        emptyAssetTitle = R.string.swap_field_asset_from_title,
        maxActionProvider = maxAssetInProvider,
        fieldValidator = getAmountInFieldValidator(),
        inputState = amountInInputState
    )

    val amountOutInput = swapAmountInputMixinFactory.create(
        coroutineScope = viewModelScope,
        tokenFlow = assetOutFlow.token().nullOnStart(),
        emptyAssetTitle = R.string.swap_field_asset_to_title,
        fiatFormatter = swapInputMixinPriceImpactFiatFormatterFactory.create(priceImpact),
        fieldValidator = getAmountOutFieldValidator()
    )

    val rateDetails: Flow<ExtendedLoadingState<String>> = quotingState.map {
        when (it) {
            is QuotingState.Loaded -> ExtendedLoadingState.Loaded(formatRate(it.quote))
            else -> ExtendedLoadingState.Loading
        }
    }
        .shareInBackground()

    val showDetails: Flow<Boolean> = quotingState.mapNotNull {
        when (it) {
            is QuotingState.Loaded -> true
            is QuotingState.Default,
            is QuotingState.Error -> false

            else -> null // Don't do anything if it's loading state
        }
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val _validationProgress = MutableStateFlow(false)

    val validationProgress = _validationProgress

    private val inputButtonState: Flow<DescriptiveButtonState> = combine(
        quotingState,
        accumulate(amountInInput.fieldError, amountOutInput.fieldError),
        accumulate(amountInInput.inputState, amountOutInput.inputState),
        assetInFlow,
        assetOutFlow,
        ::formatButtonStates
    )
        .distinctUntilChanged()
        .debounce(100)
        .shareInBackground()

    val buttonState = combine(inputButtonState, quotingState, feeSnapshot, feeMixin.fee) { inputState, quote, snapshot, fee ->
        val hasFeeForInputs = quote is QuotingState.Loaded &&
            snapshot?.matchesQuote(quote) == true && fee is FeeStatus.Loaded

        if (inputState is DescriptiveButtonState.Enabled && !hasFeeForInputs) {
            DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue))
        } else {
            inputState
        }
    }

    val swapDirectionFlipped: MutableLiveData<Event<SwapDirection>> = MutableLiveData()

    private val notEnoughAmountErrorFlow = amountInInput.fieldError.map { it.isErrorWithTag(EnoughAmountFieldValidator.ERROR_TAG) }
    val getAssetOptionsMixin = getAssetOptionsMixinFactory.create(
        assetFlow = chainAssetIn,
        additionalButtonFilter = notEnoughAmountErrorFlow,
        scope = viewModelScope,
    )

    private val amountInputFormatter = CompoundNumberFormatter(
        abbreviations = listOf(
            NumberAbbreviation(
                threshold = BigDecimal.ZERO,
                divisor = BigDecimal.ONE,
                suffix = "",
                formatter = DynamicPrecisionFormatter(minScale = 5, minPrecision = 3)
            ),
            NumberAbbreviation(
                threshold = BigDecimal.ONE,
                divisor = BigDecimal.ONE,
                suffix = "",
                formatter = FixedPrecisionFormatter(precision = 5)
            ),
        )
    )

    private var quotingJob: Job? = null

    /** Set when the user moves on to confirmation; anything else counts as leaving the flow. */
    @Volatile
    private var proceededToConfirmation = false

    init {
        analyticsService.track(AnalyticsEvent.SwapScreenOpened(payload.source.toAnalyticsSource()))

        observeWallet()
        observeSelections()
        swapRouter.observeSwapRetries().onEach { retry ->
            val account = selectedAccountUseCase.getSelectedMetaAccount()
            if (retry.metaAccountId != null && retry.metaAccountId != account.id) return@onEach
            entryPayload.value = retry
        }.launchIn(viewModelScope)

        launch { swapInteractor.warmUpSwapCommonlyUsedChains(swapFlowScope) }

        handleInputChanges(amountInInput, SwapSettings::assetIn, SwapDirection.SPECIFIED_IN)
        handleInputChanges(amountOutInput, SwapSettings::assetOut, SwapDirection.SPECIFIED_OUT)

        setupQuoting()

        setupUpdateSystem()

        feeMixin.setupFees()

        launch {
            swapInteractor.sync(swapFlowScope)
        }
    }

    fun selectPayToken() = selectToken(selectingPay = true)

    fun selectReceiveToken() = selectToken(selectingPay = false)

    fun applyTabPayload(payload: SwapSettingsPayload) {
        if (entryPayload.value === payload) return

        analyticsService.track(AnalyticsEvent.SwapScreenOpened(payload.source.toAnalyticsSource()))
        chart.onEntry(collapsed = payload != SwapSettingsPayload.Restore)
        entryPayload.value = payload
    }

    private fun selectToken(selectingPay: Boolean) = launchUnit {
        val metaAccountId = initializedMetaAccountId ?: return@launchUnit
        val settings = swapSettings.first()
        val opposite = if (selectingPay) settings.assetOut else settings.assetIn
        val selected = if (selectingPay) settings.assetIn else settings.assetOut
        val requestId = UUID.randomUUID().toString()
        selectionRequestId = requestId
        tokenSelectionCommunicator.openRequest(
            SwapTokenSelectionCommunicator.Request(requestId, selectingPay, opposite?.toAssetPayload(), selected?.toAssetPayload(), metaAccountId)
        )
    }

    private fun observeSelections() {
        tokenSelectionCommunicator.responseFlow.onEach { response ->
            if (response.requestId != selectionRequestId) return@onEach
            val asset = selectionInteractor.resolve(response.asset.fullChainAssetId) ?: return@onEach
            if (response.requestId != selectionRequestId || response.metaAccountId != initializedMetaAccountId ||
                response.metaAccountId != selectedAccountUseCase.getSelectedMetaAccount().id
            ) {
                return@onEach
            }
            selectionRequestId = null
            if (response.clearOpposite) {
                amountInInput.clearInput()
                amountOutInput.clearInput()
            }
            swapSettingsState.applyAssetSelection(asset, response.selectingPay, response.clearOpposite)
            rememberSelection()
        }.launchIn(viewModelScope)
        slippageCommunicator.responseFlow.onEach { response ->
            if (response.requestId == null || response.requestId != slippageRequestId) return@onEach
            slippageRequestId = null
            swapSettingsState.setSlippage(response.fraction.fractions)
        }.launchIn(viewModelScope)
    }

    private suspend fun rememberSelection() {
        if (display != SwapFormDisplay.TAB) return
        val metaAccountId = initializedMetaAccountId ?: return
        val settings = swapSettings.first()
        selectionInteractor.remember(metaAccountId, settings.assetIn, settings.assetOut)
    }

    fun setActive(isActive: Boolean) {
        active.value = isActive
        if (!isActive) quotingJob?.cancel()
    }

    fun setDetailsExpanded(expanded: Boolean) {
        detailsExpanded.value = expanded
    }

    fun retryQuote() = launchUnit {
        performQuote(swapSettings.first(), shouldShowLoading = true, origin = QuoteRefreshOrigin.USER_INPUT)
    }

    fun routeClicked() = launchUnit {
        currentOperationPayload()?.let(swapRouter::openSwapRoute)
    }

    fun networkFeeClicked() = launchUnit {
        currentOperationPayload()?.let(swapRouter::openSwapFee)
    }

    fun continueButtonClicked() = launchUnit {
        if (_validationProgress.value) return@launchUnit
        _validationProgress.value = true
        val payload = getValidationPayload() ?: run {
            _validationProgress.value = false
            return@launchUnit
        }
        val dustCheckFee = if (amountInInputState.value.inputKind.isMaxAction()) {
            maxAssetInProvider.dustCheckFeeFor(payload.swapQuote)
        } else {
            SwapDustCheckFee.QuoteFee
        }
        val validationSystem = swapInteractor.validationSystem(payload.metaAccountId)

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload.copy(dustCheckFee = dustCheckFee),
            progressConsumer = _validationProgress.progressConsumer(),
            validationFailureTransformerCustom = ::formatValidationFailure,
        ) { validPayload ->
            _validationProgress.value = false

            proceededToConfirmation = true

            trackSwapInitiated(entryPayload.value.source.toAnalyticsSource())
            openSwapConfirmation(validPayload)
        }
    }

    fun rateDetailsClicked() {
        val mode = (quotingState.value as? QuotingState.Loaded)?.quote?.swapRateDescriptionMode()
            ?: SwapRateDescriptionMode.Default
        launchSwapRateDescription(resourceManager, mode)
    }

    fun flipAssets() = launch {
        val previousSettings = swapSettings.first()
        val newSettings = swapSettingsState.flipAssets()

        applyFlipToUi(previousSettings, newSettings)
        rememberSelection()
    }

    fun openOptions() = launchUnit {
        val settings = swapSettings.first()
        val asset = settings.assetIn ?: settings.assetOut
        val config = asset?.let { swapInteractor.slippageConfig(it.chainId) }
            ?: SlippageConfig.default()
        val requestId = UUID.randomUUID().toString()
        slippageRequestId = requestId
        slippageCommunicator.openRequest(
            SlippageSettingsPayload(
                selected = settings.slippage.inFraction,
                default = config.defaultSlippage.inFraction,
                tips = config.slippageTips.map { it.inFraction },
                minimum = config.minAvailableSlippage.inFraction,
                maximum = config.maxAvailableSlippage.inFraction,
                small = config.smallSlippage.inFraction,
                big = config.bigSlippage.inFraction,
                requestId = requestId
            )
        )
    }

    fun backClicked() {
        swapRouter.back()
    }

    private fun trackSwapInitiated(source: SwapSource) = launch {
        val quote = (quotingState.value as? QuotingState.Loaded)?.quote ?: return@launch

        // fiat estimation is honestly unavailable without a token rate - skip the event in that case
        val usdAmount = usdRateRepository.planksToUsd(quote.assetIn, quote.planksIn)

        val assetInSymbol = quote.assetIn.symbol.value
        val assetOutSymbol = quote.assetOut.symbol.value

        analyticsService.track(
            AnalyticsEvent.SwapInitiated(
                source = source,
                assetInCategory = AssetCategoryClassifier.classify(assetInSymbol),
                assetOutCategory = AssetCategoryClassifier.classify(assetOutSymbol),
                assetIn = assetInSymbol,
                assetOut = assetOutSymbol,
                networkIn = chainRegistry.getChain(quote.assetIn.chainId).name,
                networkOut = chainRegistry.getChain(quote.assetOut.chainId).name,
                amountBucket = AmountBucket.fromOrUnknown(usdAmount)
            )
        )
    }

    private fun openSwapConfirmation(validPayload: SwapValidationPayload) = launchUnit {
        swapRouter.openSwapConfirmation(
            operationMapper.toPayload(
                state = validPayload.toSwapState(),
                metaAccountId = requireNotNull(validPayload.metaAccountId),
                returnToTab = display == SwapFormDisplay.TAB
            )
        )
    }

    private suspend fun currentOperationPayload(): SwapOperationPayload? {
        val payload = getValidationPayload() ?: return null
        return operationMapper.toPayload(payload.toSwapState(), requireNotNull(payload.metaAccountId), returnToTab = display == SwapFormDisplay.TAB)
    }

    private fun createMaxActionProvider(): SwapMaxActionProvider {
        return maxActionProviderFactory.create(
            scope = viewModelScope,
            assets = assetInFlow.filterNotNull(),
            outputAssets = swapSettings.map { it.assetOut },
            maxActive = amountInInputState.map { it.inputKind.isMaxAction() }.distinctUntilChanged(),
            feeLoaderMixin = feeMixin,
            feeResetVersion = maxFeeResetVersion
        )
    }

    private fun observeWallet() {
        combine(
            selectedAccountUseCase.selectedMetaAccountFlow().distinctUntilChangedBy { it.id },
            entryPayload
        ) { metaAccount, payload -> metaAccount to payload }
            .mapLatest { (metaAccount, payload) ->
                initializedMetaAccountId = null
                feeSnapshot.value = null
                selectionRequestId = null
                slippageRequestId = null
                quotingJob?.cancel()
                quotingState.value = QuotingState.Default
                amountInInput.clearInput()
                amountOutInput.clearInput()
                swapSettingsState.setSwapSettings(SwapSettings(slippage = DEFAULT_SLIPPAGE))
                feeMixin.setFeeStatus(FeeStatus.NoFee)
                maxFeeResetVersion.update { it + 1 }

                val selection = if (payload == SwapSettingsPayload.Restore) {
                    selectionInteractor.initialSelection(metaAccount.id)
                } else {
                    val assetIn = selectionInteractor.resolve(payload.assetIn?.fullChainAssetId)
                    val assetOutPayload = when (payload) {
                        is SwapSettingsPayload.Prefilled -> payload.assetOut
                        is SwapSettingsPayload.RepeatOperation -> payload.assetOut
                        else -> null
                    }
                    SwapSelectionInteractor.Selection(assetIn, selectionInteractor.resolve(assetOutPayload?.fullChainAssetId))
                }
                val retry = (payload as? SwapSettingsPayload.RepeatOperation)?.takeIf { it.metaAccountId == null || it.metaAccountId == metaAccount.id }
                initializedMetaAccountId = metaAccount.id
                swapSettingsState.setSwapSettings(
                    SwapSettings(
                        assetIn = selection.assetIn,
                        assetOut = selection.assetOut,
                        amount = retry?.amount,
                        swapDirection = retry?.direction?.mapFromModel(),
                        slippage = retry?.slippage?.fractions ?: DEFAULT_SLIPPAGE
                    )
                )
                if (retry != null && selection.assetIn != null && selection.assetOut != null) {
                    initInputSilently(retry.direction.mapFromModel(), selection.assetIn, selection.assetOut, retry.amount)
                }
            }.launchIn(viewModelScope)
    }

    private fun initInputSilently(direction: SwapDirection, assetIn: Chain.Asset, assetOut: Chain.Asset, amount: Balance) {
        when (direction) {
            SwapDirection.SPECIFIED_IN -> {
                amountInInput.updateInput(assetIn, amount)
            }

            SwapDirection.SPECIFIED_OUT -> {
                amountOutInput.updateInput(assetOut, amount)
            }
        }
    }

    private fun setupUpdateSystem() = launch {
        swapInteractor.getUpdateSystem(originChainFlow, swapSettingsState, swapFlowScope)
            .start()
            .launchIn(viewModelScope)
    }

    private fun FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>.setupFees() {
        quotingState
            .onEach {
                when (it) {
                    is QuotingState.Loading -> setFeeLoading()
                    is QuotingState.Error, QuotingState.Default -> setFeeStatus(FeeStatus.NoFee)
                    else -> {}
                }
            }
            .throttleFeeRefreshes()
            .onEach { loadSwapFee(it) }
            .inBackground()
            .launchIn(viewModelScope)
    }

    private fun FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>.loadSwapFee(quoteState: QuotingState.Loaded) {
        loadFee { feePaymentCurrency ->
            val swapArgs = quoteState.quote.toExecuteArgs(
                slippage = quoteState.settings.slippage,
                firstSegmentFees = feePaymentCurrency,
                metaAccountId = quoteState.quoteArgs.metaAccountId
            )

            val fee = swapInteractor.estimateFee(swapArgs)
            val snapshot = SwapFeeSnapshot(quoteState, fee)
            val settings = swapSettings.first()
            val account = selectedAccountUseCase.getSelectedMetaAccount()
            coroutineContext.ensureActive()
            // Market quotes can change during estimation; only changed form inputs invalidate this result.
            if (!snapshot.matchesInputs(settings, account.id)) return@loadFee null

            feeSnapshot.value = snapshot
            fee
        }
    }

    private fun applyFlipToUi(previousSettings: SwapSettings, newSettings: SwapSettings) {
        val swapDirection = previousSettings.swapDirection ?: return
        val amount = previousSettings.amount
        if (amount == null) {
            swapDirection.clearCalculatedInput(amountInInput, amountOutInput)
            return
        }

        when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> {
                val previousIn = previousSettings.assetIn ?: return
                amountOutInput.updateInput(previousIn, amount)
                amountInInput.clearInput()
            }

            SwapDirection.SPECIFIED_OUT -> {
                val previousOut = previousSettings.assetOut ?: return
                amountInInput.updateInput(previousOut, amount)
                amountOutInput.clearInput()
            }
        }

        swapDirectionFlipped.value = newSettings.swapDirection!!.event()
    }

    private fun formatRate(swapQuote: SwapQuote): String {
        val rate = swapQuote.swapRate()
        return swapRateFormatter.format(rate, swapQuote.assetIn, swapQuote.assetOut)
    }

    private fun formatButtonStates(
        quotingState: QuotingState,
        errorStates: List<FieldValidationResult>,
        inputs: List<InputState<String>>,
        assetIn: Asset?,
        assetOut: Asset?,
    ): DescriptiveButtonState {
        return when {
            assetIn == null -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.swap_main_settings_asset_in_not_selecting_button_state))
            }

            assetOut == null -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.swap_main_settings_asset_out_not_selecting_button_state))
            }

            inputs.all { it.value.isEmpty() } -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.swap_main_settings_enter_amount_disabled_button_state))
            }

            errorStates.any { it.isErrorWithTag(EnoughAmountFieldValidator.ERROR_TAG) } -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.swap_not_enough_asset, assetIn.token.configuration.symbol.value))
            }

            errorStates.any { it is FieldValidationResult.Error } -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.swap_main_settings_wrong_amount_disabled_button_state))
            }

            quotingState is QuotingState.Loading -> DescriptiveButtonState.Loading

            quotingState is QuotingState.Error || inputs.any { it.value.isEmpty() } -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue))
            }

            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    private fun setupQuoting() {
        setupPerSwapSettingQuoting()

        setupSubscriptionQuoting()
    }

    private fun setupPerSwapSettingQuoting() {
        var previousSettings: SwapSettings? = null
        combine(swapSettings, active) { settings, isActive -> settings to isActive }
            .onEach { (settings, isActive) ->
                if (settings != swapSettings.first()) return@onEach

                settings.invalidateCalculatedInput(previousSettings, amountInInput, amountOutInput)
                previousSettings = settings

                if (isActive) performQuote(settings, shouldShowLoading = true, origin = QuoteRefreshOrigin.USER_INPUT)
            }.launchIn(viewModelScope)
    }

    private fun setupSubscriptionQuoting() {
        combine(selectedAccountUseCase.selectedMetaAccountFlow(), active) { account, isActive -> account to isActive }
            .flatMapLatest { (account, isActive) ->
                if (isActive) swapInteractor.runSubscriptions(account) else emptyFlow()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(this@SwapMainSettingsViewModel.LOG_TAG, "Failure during subscriptions run", error)
            }
            .onEach {
                val state = quotingState.value
                if ((state is QuotingState.Loaded || state is QuotingState.Error) && quotingJob?.isActive != true) {
                    performQuote(swapSettings.first(), shouldShowLoading = false, origin = QuoteRefreshOrigin.SUBSCRIPTION)
                }
            }.launchIn(viewModelScope)
    }

    private fun performQuote(settings: SwapSettings, shouldShowLoading: Boolean, origin: QuoteRefreshOrigin) {
        if (!active.value) return

        quotingJob?.cancel()
        if (shouldShowLoading) {
            feeSnapshot.value = null
            quotingState.value = QuotingState.Loading
        }
        quotingJob = launch {
            val swapQuoteArgs = settings.toQuoteArgs(
                tokenIn = { assetInFlow.ensureToken(it) },
                tokenOut = { assetOutFlow.ensureToken(it) },
            ) ?: run {
                quotingState.value = QuotingState.Default
                return@launch
            }

            val quote = withTimeoutOrNull(QUOTE_LOADING_TIMEOUT) {
                if (BuildConfig.DEBUG) {
                    Log.d("Swaps", "Quote requested: origin=$origin, direction=${swapQuoteArgs.swapDirection}, amountPlanks=${swapQuoteArgs.amount}")
                }
                swapInteractor.quote(swapQuoteArgs, swapFlowScope)
            } ?: Result.failure(IOException("Swap quote timed out"))

            val selectedMetaAccountId = selectedAccountUseCase.getSelectedMetaAccount().id
            val currentSettings = swapSettings.first()
            coroutineContext.ensureActive()
            // A quote can finish after the form changes or before it resets for the newly selected wallet.
            if (!active.value || settings != currentSettings) return@launch
            if (swapQuoteArgs.metaAccountId != selectedMetaAccountId || selectedMetaAccountId != initializedMetaAccountId) return@launch

            quotingState.value = quote.fold(
                onSuccess = { QuotingState.Loaded(it, swapQuoteArgs, origin, settings) },
                onFailure = {
                    if (it is CancellationException) {
                        throw it
                    } else {
                        QuotingState.Error(it)
                    }
                }
            )

            handleNewQuote(quote, settings)
        }
    }

    private fun QuotingState.toExecutionEstimate(): ExtendedLoadingState<String?> {
        return toLoadingState {
            val estimatedDuration = it.executionEstimate.totalTime()
            resourceManager.formatDuration(estimatedDuration, estimated = true)
        }
    }

    private fun handleNewQuote(quoteResult: Result<SwapQuote>, swapSettings: SwapSettings) {
        quoteResult.onSuccess { quote ->
            when (swapSettings.swapDirection!!) {
                SwapDirection.SPECIFIED_IN -> amountOutInput.updateInput(quote.assetOut, quote.planksOut)
                SwapDirection.SPECIFIED_OUT -> amountInInput.updateInput(quote.assetIn, quote.planksIn)
            }
        }.onFailure {
            swapSettings.swapDirection.clearCalculatedInput(amountInInput, amountOutInput)
        }
    }

    private inline fun SwapSettings.toQuoteArgs(
        tokenIn: (Chain.Asset) -> Token,
        tokenOut: (Chain.Asset) -> Token
    ): SwapQuoteArgs? {
        val assetIn = assetIn ?: return null
        val assetOut = assetOut ?: return null
        val direction = swapDirection ?: return null
        val amount = amount?.takeIf { it.isPositive() } ?: return null
        val metaAccountId = initializedMetaAccountId

        return SwapQuoteArgs(
            tokenIn = tokenIn(assetIn),
            tokenOut = tokenOut(assetOut),
            amount = amount,
            swapDirection = direction,
            metaAccountId = metaAccountId,
        )
    }

    private fun handleInputChanges(
        amountInput: SwapAmountInputMixin.Presentation,
        chainAsset: (SwapSettings) -> Chain.Asset?,
        swapDirection: SwapDirection
    ) {
        amountInput.amountState
            .filter { it.initiatedByUser }
            .onEach { state ->
                val settings = swapSettings.first()
                val asset = chainAsset(settings) ?: return@onEach
                val planks = state.value?.let(asset::planksFromAmount)
                if (planks == null && settings.amount == null) return@onEach
                if (BuildConfig.DEBUG && state.inputKind == InputKind.MAX_ACTION) {
                    Log.d("Swaps", "Max input: previousPlanks=${settings.amount}, amountPlanks=$planks")
                }
                swapSettingsState.setAmount(planks, swapDirection)
            }.launchIn(viewModelScope)
    }

    private fun SwapAmountInputMixin.updateInput(chainAsset: Chain.Asset, planks: Balance) {
        val amount = chainAsset.amountFromPlanks(planks)
        // In editable amounts a comma always means a decimal separator.
        val ungroupedAmount = amountInputFormatter.format(amount).replace(",", "")
        inputState.value = InputState(ungroupedAmount, initiatedByUser = false, InputKind.REGULAR)
    }

    private fun Flow<SwapSettings>.assetFlowOf(extractor: (SwapSettings) -> Chain.Asset?): Flow<Asset?> {
        return map { extractor(it) }
            .transformLatest { chainAsset ->
                if (chainAsset == null) {
                    emit(null)
                } else {
                    emitAll(assetUseCase.assetFlow(chainAsset))
                }
            }
            .shareInBackground()
    }

    private suspend fun Flow<Asset?>.ensureToken(asset: Chain.Asset): Token {
        return filterNotNull()
            .first { it.token.configuration.fullId == asset.fullId }
            .token
    }

    private fun getAmountInFieldValidator(): FieldValidator {
        return CompoundFieldValidator(
            enoughAmountValidatorFactory.create(
                maxAssetInProvider.affordableBalanceProvider,
                errorMessageRes = R.string.swap_field_validation_not_enough_amount_to_swap
            ),
            liquidityFieldValidatorFactory.create(quotingState)
        )
    }

    private fun getAmountOutFieldValidator(): FieldValidator {
        return swapReceiveAmountAboveEDFieldValidatorFactory.create(assetOutFlow)
    }

    private suspend fun getValidationPayload(): SwapValidationPayload? {
        val settings = swapSettings.first()
        val account = selectedAccountUseCase.getSelectedMetaAccount()
        val quote = quotingState.value as? QuotingState.Loaded ?: return null
        val snapshot = feeSnapshot.value ?: return null
        if (!snapshot.matchesInputs(settings, account.id) || !snapshot.matchesQuote(quote)) return null
        if (feeMixin.fee.value !is FeeStatus.Loaded) return null

        return snapshot.toValidationPayload()
    }

    private fun formatValidationFailure(
        status: ValidationStatus.NotValid<SwapValidationFailure>,
        actions: ValidationFlowActions<SwapValidationPayload>
    ) = mapSwapValidationFailureToUI(
        resourceManager = resourceManager,
        status = status,
        actions = actions,
        amountInSwapMaxAction = ::setMaxAvailableAmountIn,
        amountOutSwapMinAction = ::setMinAmountOut,
        amountInSetAction = ::setAmountIn,
    )

    private fun setMaxAvailableAmountIn() {
        launch {
            amountInInput.invokeMaxClick()
        }
    }

    private fun setMinAmountOut(chainAsset: Chain.Asset, amountInPlanks: Balance) {
        amountOutInput.requestFocusLiveData.sendEvent()

        // initiatedByUser = true so the change passes the initiatedByUser filter in handleInputChanges
        // and triggers a re-quote - it is a direct reaction to the user's tap in the dialog
        amountOutInput.setAmount(chainAsset.amountFromPlanks(amountInPlanks), initiatedByUser = true)
    }

    private fun setAmountIn(chainAsset: Chain.Asset, amountInPlanks: Balance) {
        amountInInput.requestFocusLiveData.sendEvent()

        // initiatedByUser = true so the change passes the initiatedByUser filter in handleInputChanges
        // and triggers a re-quote - it is a direct reaction to the user's tap in the dialog
        amountInInput.setAmount(chainAsset.amountFromPlanks(amountInPlanks), initiatedByUser = true)
    }

    private fun Flow<Asset?>.token(): Flow<Token?> = map { it?.token }

    override fun onCleared() {
        if (!proceededToConfirmation) {
            analyticsService.track(AnalyticsEvent.SwapAbandoned(SwapStage.SETUP))
        }

        super.onCleared()
    }
}

internal fun SwapSettings.invalidateCalculatedInput(
    previousSettings: SwapSettings?,
    amountInInput: SwapAmountInputMixin,
    amountOutInput: SwapAmountInputMixin,
) {
    val amountInputsChanged = assetIn != previousSettings?.assetIn || assetOut != previousSettings?.assetOut ||
        amount != previousSettings?.amount || swapDirection != previousSettings?.swapDirection

    if (amountInputsChanged) {
        swapDirection.clearCalculatedInput(amountInInput, amountOutInput)
    }
}

private fun SwapDirection?.clearCalculatedInput(amountInInput: SwapAmountInputMixin, amountOutInput: SwapAmountInputMixin) {
    when (this) {
        SwapDirection.SPECIFIED_IN -> amountOutInput.clearInput()
        SwapDirection.SPECIFIED_OUT -> amountInInput.clearInput()
        null -> Unit
    }
}

private fun SwapAmountInputMixin.clearInput() {
    inputState.value = InputState(value = "", initiatedByUser = false, inputKind = InputKind.REGULAR)
}
