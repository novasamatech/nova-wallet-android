package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.accumulate
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.formatting.CompoundNumberFormatter
import io.novafoundation.nova.common.utils.formatting.DynamicPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.FixedPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.NumberAbbreviation
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.nullOnStart
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.utils.skipFirst
import io.novafoundation.nova.common.utils.zipWithPrevious
import io.novafoundation.nova.common.validation.CompoundFieldValidator
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.description.launchNetworkFeeDescription
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.mapFromModel
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.model.GetAssetInOption
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fee.SwapFeeFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fee.SwapFeeInspector
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteState
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapState
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.EnoughAmountToSwapValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.LiquidityFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.SwapReceiveAmountAboveEDFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapInputMixinPriceImpactFiatFormatterFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.view.GetAssetInBottomSheet
import io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_swap_impl.presentation.state.swapSettingsFlow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.AmountErrorState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.invokeMaxClick
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.PaymentCurrencySelectionMode
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val payload: SwapSettingsPayload,
    private val validationExecutor: ValidationExecutor,
    private val liquidityFieldValidatorFactory: LiquidityFieldValidatorFactory,
    private val swapReceiveAmountAboveEDFieldValidatorFactory: SwapReceiveAmountAboveEDFieldValidatorFactory,
    private val enoughAmountToSwapValidatorFactory: EnoughAmountToSwapValidatorFactory,
    private val swapInputMixinPriceImpactFiatFormatterFactory: SwapInputMixinPriceImpactFiatFormatterFactory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val buyMixinFactory: BuyMixin.Factory,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val swapRateFormatter: SwapRateFormatter,
    private val swapRouteFormatter: SwapRouteFormatter,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    private val swapStateStoreProvider: SwapStateStoreProvider,
    swapAmountInputMixinFactory: SwapAmountInputMixinFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    actionAwaitableFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel(),
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    Validatable by validationExecutor {

    private val swapSettingState = async {
        swapSettingsStateProvider.getSwapSettingsState(viewModelScope)
    }

    private val swapSettings = swapSettingsStateProvider.swapSettingsFlow(viewModelScope)
        .share()

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

    val swapRouteState = quotingState
        .map { quoteState -> quoteState.toSwapRouteState() }
        .shareInBackground()

    private val originChainFlow = swapSettings
        .mapNotNull { it.assetIn?.chainId }
        .distinctUntilChanged()
        .map { chainRegistry.getChain(it) }
        .shareInBackground()

    val feeMixin = feeLoaderMixinFactory.create(
        scope = viewModelScope,
        selectedChainAssetFlow = swapSettings.mapNotNull { it.assetIn },
        feeFormatter = SwapFeeFormatter(swapInteractor),
        feeInspector = SwapFeeInspector(),
        configuration = Configuration(
            initialState = Configuration.InitialState(
                paymentCurrencySelectionMode = PaymentCurrencySelectionMode.AUTOMATIC_ONLY
            )
        )
    )

    val buyMixin = buyMixinFactory.create(viewModelScope)

    val amountInInput = swapAmountInputMixinFactory.create(
        coroutineScope = viewModelScope,
        tokenFlow = assetInFlow.token().nullOnStart(),
        emptyAssetTitle = R.string.swap_field_asset_from_title,
        maxActionProvider = createMaxActionProvider(),
        fieldValidator = getAmountInFieldValidator()
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

    val buttonState: Flow<DescriptiveButtonState> = combine(
        quotingState,
        accumulate(amountInInput.fieldError, amountOutInput.fieldError),
        accumulate(amountInInput.inputState, amountOutInput.inputState),
        assetInFlow,
        assetOutFlow,
        ::formatButtonStates
    ).distinctUntilChanged()
        .debounce(100)

    val swapDirectionFlipped: MutableLiveData<Event<SwapDirection>> = MutableLiveData()

    private val getAssetInOptions = swapInteractor.availableGetAssetInOptionsFlow(chainAssetIn)
        .shareInBackground()

    val getAssetInOptionsButtonState = combine(
        assetInFlow,
        getAssetInOptions,
        amountInInput.amountState
    ) { assetIn, getAssetInOptions, amountState ->
        if (assetIn == null) return@combine DescriptiveButtonState.Gone
        val amount = amountState.value.orZero()

        val balanceOverTransferable = amount > assetIn.transferable || assetIn.transferable.isZero

        if (balanceOverTransferable && getAssetInOptions.isNotEmpty()) {
            val symbol = assetIn.token.configuration.symbol
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_get_token_format, symbol))
        } else {
            DescriptiveButtonState.Gone
        }
    }
        .onStart { emit(DescriptiveButtonState.Gone) }
        .distinctUntilChanged()
        .shareInBackground()

    val selectGetAssetInOption = actionAwaitableFactory.create<GetAssetInBottomSheet.Payload, GetAssetInOption>()

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

    init {
        initPayload()

        launch { swapInteractor.warmUpSwapCommonlyUsedChains(viewModelScope) }

        handleInputChanges(amountInInput, SwapSettings::assetIn, SwapDirection.SPECIFIED_IN)
        handleInputChanges(amountOutInput, SwapSettings::assetOut, SwapDirection.SPECIFIED_OUT)

        setupQuoting()

        setupUpdateSystem()

        feeMixin.setupFees()

        launch {
            swapInteractor.sync(viewModelScope)
        }
    }

    fun selectPayToken() {
        launch {
            val outAsset = assetOutFlow.firstOrNull()
                ?.token
                ?.configuration
            val payload = outAsset?.let { AssetPayload(it.chainId, it.id) }

            swapRouter.selectAssetIn(payload)
        }
    }

    fun selectReceiveToken() {
        launch {
            val inAsset = assetInFlow.firstOrNull()
                ?.token
                ?.configuration
            val payload = inAsset?.let { AssetPayload(it.chainId, it.id) }

            swapRouter.selectAssetOut(payload)
        }
    }

    fun continueButtonClicked() {
        launch {
            val quotingState = quotingState.value
            if (quotingState !is QuotingState.Loaded) return@launch

            val swapState = SwapState(
                quote = quotingState.quote,
                fee = feeMixin.awaitFee(),
                slippage = swapSettings.first().slippage
            )
            swapStateStoreProvider.getStore(viewModelScope).setState(swapState)
            swapRouter.openSwapConfirmation()

//            val validationSystem = swapInteractor.validationSystem()
//            val payload = getValidationPayload() ?: return@launch
//
//            validationExecutor.requireValid(
//                validationSystem = validationSystem,
//                payload = payload,
//                progressConsumer = _validationProgress.progressConsumer(),
//                validationFailureTransformerCustom = ::formatValidationFailure,
//            ) { validPayload ->
//                _validationProgress.value = false
//                openSwapConfirmation(validPayload)
//            }
        }
    }

    fun rateDetailsClicked() {
        launchSwapRateDescription()
    }

    fun networkFeeClicked() {
        launchNetworkFeeDescription()
    }

    fun flipAssets() = launch {
        val previousSettings = swapSettings.first()
        val newSettings = swapSettingState().flipAssets()

        applyFlipToUi(previousSettings, newSettings)
    }

    fun getAssetInClicked() = launch {
        val assetIn = chainAssetIn.first() ?: return@launch
        val availableOptions = getAssetInOptions.first()

        val payload = GetAssetInBottomSheet.Payload(
            chainAsset = assetIn,
            availableOptions = availableOptions
        )

        val selectedOption = selectGetAssetInOption.awaitAction(payload)
        onGetAssetInOptionSelected(selectedOption)
    }

    fun openOptions() {
        swapRouter.openSwapOptions()
    }

    fun backClicked() {
        swapRouter.back()
    }

    private fun createMaxActionProvider(): MaxActionProvider {
        return maxActionProviderFactory.create(
            assetInFlow = assetInFlow,
            assetOutFlow = assetOutFlow,
            field = Asset::transferableInPlanks,
            feeLoaderMixin = feeMixin,
        )
    }

    private fun onGetAssetInOptionSelected(option: GetAssetInOption) {
        when (option) {
            GetAssetInOption.RECEIVE -> receiveSelected()
            GetAssetInOption.CROSS_CHAIN -> onCrossChainTransferSelected()
            GetAssetInOption.BUY -> buySelected()
        }
    }

    private fun onCrossChainTransferSelected() = launch {
        val chainAssetIn = chainAssetIn.first() ?: return@launch
        val assetInChain = originChainFlow.first()

        val currentAddress = selectedAccountUseCase.getSelectedMetaAccount().addressIn(assetInChain)

        swapRouter.openSendCrossChain(AssetPayload(chainAssetIn.chainId, chainAssetIn.id), currentAddress)
    }

    private fun buySelected() = launch {
        val chainAssetIn = chainAssetIn.first() ?: return@launch
        buyMixin.buyClicked(chainAssetIn)
    }

    private fun receiveSelected() = launch {
        val chainAssetIn = chainAssetIn.first() ?: return@launch
        swapRouter.openReceive(AssetPayload(chainAssetIn.chainId, chainAssetIn.id))
    }

    private fun initPayload() {
        launch {
            val assetIn = chainRegistry.asset(payload.assetIn.fullChainAssetId)
            val swapSettingsState = swapSettingState.await()
            when (payload) {
                is SwapSettingsPayload.DefaultFlow -> swapSettingState().setAssetIn(assetIn)

                is SwapSettingsPayload.RepeatOperation -> {
                    val assetOut = chainRegistry.asset(payload.assetOut.fullChainAssetId)
                    val oldSwapSettings = swapSettingsState.selectedOption.first()
                    val direction = payload.direction.mapFromModel()

                    val swapSettings = SwapSettings(
                        assetIn = assetIn,
                        assetOut = assetOut,
                        amount = payload.amount,
                        swapDirection = direction,
                        slippage = oldSwapSettings.slippage
                    )

                    swapSettingsState.setSwapSettings(swapSettings)

                    initInputSilently(direction, assetIn, assetOut, payload.amount)
                }
            }
        }
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
        swapInteractor.getUpdateSystem(originChainFlow, viewModelScope)
            .start()
            .launchIn(viewModelScope)
    }

    private fun FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>.setupFees() {
        quotingState
            .onEach {
                when (it) {
                    is QuotingState.Loading -> setFeeLoading()
                    is QuotingState.Error -> setFeeStatus(FeeStatus.NoFee)
                    else -> {}
                }
            }
            .filterIsInstance<QuotingState.Loaded>()
            .debounce(300.milliseconds)
            .zipWithPrevious()
            .mapNotNull { (previous, current) ->
                current.takeIf {
                    // allow same value in case user quickly switched from this value to another and back without waiting for fee loading
                    previous != current || feeMixin.fee.value !is FeeStatus.Loaded
                }
            }
            .onEach { quoteState ->
                loadFee { feePaymentCurrency ->
                    val swapArgs = quoteState.quote.toExecuteArgs(
                        slippage = swapSettings.first().slippage,
                        firstSegmentFees = feePaymentCurrency
                    )

                    swapInteractor.estimateFee(swapArgs)
                }
            }
            .inBackground()
            .launchIn(viewModelScope)
    }

    private fun applyFlipToUi(previousSettings: SwapSettings, newSettings: SwapSettings) {
        val amount = previousSettings.amount ?: return
        val swapDirection = previousSettings.swapDirection ?: return

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
        return swapRateFormatter.format(swapQuote.swapRate(), swapQuote.assetIn, swapQuote.assetOut)
    }

    private fun formatButtonStates(
        quotingState: QuotingState,
        errorStates: List<AmountErrorState>,
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

            errorStates.any { it is AmountErrorState.Invalid } -> {
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
        swapSettings.mapLatest { performQuote(it, shouldShowLoading = true) }
            .launchIn(viewModelScope)
    }

    private fun setupSubscriptionQuoting() {
        flowOfAll {
            swapInteractor.runSubscriptions(selectedAccountUseCase.getSelectedMetaAccount())
                .catch { Log.e(this@SwapMainSettingsViewModel.LOG_TAG, "Failure during subscriptions run", it) }
        }.onEach {
            Log.d("Swap", "ReQuote triggered from subscription")

            val currentSwapSettings = swapSettings.first()

            performQuote(currentSwapSettings, shouldShowLoading = false)
        }.launchIn(viewModelScope)
    }

    private suspend fun performQuote(swapSettings: SwapSettings, shouldShowLoading: Boolean) {
        val swapQuoteArgs = swapSettings.toQuoteArgs(
            tokenIn = { assetInFlow.ensureToken(it) },
            tokenOut = { assetOutFlow.ensureToken(it) },
        ) ?: return

        if (shouldShowLoading) {
            quotingState.value = QuotingState.Loading
        }

        val quote = swapInteractor.quote(swapQuoteArgs, viewModelScope)

        quotingState.value = quote.fold(
            onSuccess = { QuotingState.Loaded(it, swapQuoteArgs) },
            onFailure = {
                if (it is CancellationException) {
                    QuotingState.Loading
                } else {
                    QuotingState.Error(it)
                }
            }
        )

        handleNewQuote(quote, swapSettings)
    }

    private suspend fun QuotingState.toSwapRouteState(): SwapRouteState {
        return when(this) {
            QuotingState.Default -> ExtendedLoadingState.Loaded(null)
            is QuotingState.Error -> ExtendedLoadingState.Error(error)
            is QuotingState.Loaded -> ExtendedLoadingState.Loaded(swapRouteFormatter.formatSwapRoute(quote))
            QuotingState.Loading -> ExtendedLoadingState.Loading
        }
    }

    private fun handleNewQuote(quoteResult: Result<SwapQuote>, swapSettings: SwapSettings) {
        quoteResult.onSuccess { quote ->
            when (swapSettings.swapDirection!!) {
                SwapDirection.SPECIFIED_IN -> amountOutInput.updateInput(quote.assetOut, quote.planksOut)
                SwapDirection.SPECIFIED_OUT -> amountInInput.updateInput(quote.assetIn, quote.planksIn)
            }
        }.onFailure {
            when (swapSettings.swapDirection!!) {
                SwapDirection.SPECIFIED_OUT -> amountInInput.clearInput()
                SwapDirection.SPECIFIED_IN -> amountOutInput.clearInput()
            }
        }
    }

    private inline fun SwapSettings.toQuoteArgs(
        tokenIn: (Chain.Asset) -> Token,
        tokenOut: (Chain.Asset) -> Token
    ): SwapQuoteArgs? {
        return if (assetIn != null && assetOut != null && amount != null && swapDirection != null) {
            SwapQuoteArgs(
                tokenIn = tokenIn(assetIn!!),
                tokenOut = tokenOut(assetOut!!),
                amount = amount!!,
                swapDirection = swapDirection!!,
            )
        } else {
            null
        }
    }

    private fun handleInputChanges(
        amountInput: SwapAmountInputMixin.Presentation,
        chainAsset: (SwapSettings) -> Chain.Asset?,
        swapDirection: SwapDirection
    ) {
        amountInput.amountState
            .filter { it.initiatedByUser }
            .skipFirst()
            .onEach { state ->
                val asset = chainAsset(swapSettings.first()) ?: return@onEach
                val planks = state.value?.let(asset::planksFromAmount)
                swapSettingState().setAmount(planks, swapDirection)
            }.launchIn(viewModelScope)
    }

    private fun SwapAmountInputMixin.clearInput() {
        inputState.value = InputState(value = "", initiatedByUser = false, inputKind = InputKind.REGULAR)
    }

    private fun SwapAmountInputMixin.updateInput(chainAsset: Chain.Asset, planks: Balance) {
        val amount = chainAsset.amountFromPlanks(planks)
        inputState.value = InputState(amountInputFormatter.format(amount), initiatedByUser = false, InputKind.REGULAR)
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
            enoughAmountToSwapValidatorFactory.create(assetInFlow),
            liquidityFieldValidatorFactory.create(quotingState)
        )
    }

    private fun getAmountOutFieldValidator(): FieldValidator {
        return swapReceiveAmountAboveEDFieldValidatorFactory.create(assetOutFlow)
    }

//    private suspend fun getValidationPayload(): SwapValidationPayload? {
//        val quotingState = quotingState.value
//        if (quotingState !is QuotingState.Loaded) return null
//        val swapSettings = swapSettings.first()
//        return swapInteractor.getValidationPayload(
//            assetIn = swapSettings.assetIn ?: return null,
//            assetOut = swapSettings.assetOut ?: return null,
//            feeAsset = swapSettings.feeAsset ?: return null,
//            quoteArgs = quotingState.quoteArgs,
//            swapQuote = quotingState.value,
//            swapFee = feeMixin.loadedDecimalFeeOrNullFlow().first() ?: return null
//        )
//    }

//    private fun formatValidationFailure(
//        status: ValidationStatus.NotValid<SwapValidationFailure>,
//        actions: ValidationFlowActions<SwapValidationPayload>
//    ) = viewModelScope.mapSwapValidationFailureToUI(
//        resourceManager,
//        status,
//        actions,
//        ::setFee,
//        ::setMaxAvailableAmountIn,
//        ::setMinAmountOut,
//    )

    private fun setFee(swapFee: SwapFee) {
        launch {
            feeMixin.setFee(swapFee)
        }
    }

    private fun setMaxAvailableAmountIn() {
        launch {
            amountInInput.invokeMaxClick()
        }
    }

    private fun setMinAmountOut(chainAsset: Chain.Asset, amountInPlanks: Balance) {
        launch {
            amountOutInput.requestFocusLiveData.sendEvent()
            amountOutInput.setAmount(chainAsset.amountFromPlanks(amountInPlanks))
        }
    }

    private fun Flow<Asset?>.token(): Flow<Token?> = map { it?.token }
}
