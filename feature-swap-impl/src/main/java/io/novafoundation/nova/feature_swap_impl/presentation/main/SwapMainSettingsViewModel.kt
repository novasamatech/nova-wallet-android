package io.novafoundation.nova.feature_swap_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.CompoundNumberFormatter
import io.novafoundation.nova.common.utils.formatting.DynamicPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.FixedPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.NumberAbbreviation
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.nullOnStart
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.quotedBalance
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixinFactory
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.presentation.state.swapSettingsFlow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.milliseconds

sealed class QuotingState {

    object Loading : QuotingState()

    object NotAvailable : QuotingState()

    class Loaded(val value: SwapQuote, val quoteArgs: SwapQuoteArgs, val feeAsset: Chain.Asset) : QuotingState()
}

class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val swapAmountInputMixinFactory: SwapAmountInputMixinFactory,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val payload: SwapSettingsPayload
) : BaseViewModel() {

    private val swapSettingState = async {
        swapSettingsStateProvider.getSwapSettingsState(viewModelScope)
    }

    private val swapSettings = swapSettingsStateProvider.swapSettingsFlow(viewModelScope)

    private val quotingState = MutableStateFlow<QuotingState>(QuotingState.NotAvailable)

    private val assetOutFlow = swapSettings.assetFlowOf(SwapSettings::assetOut)
    private val assetInFlow = swapSettings.assetFlowOf(SwapSettings::assetIn)
    private val feeAssetFlow = swapSettings.assetFlowOf(SwapSettings::feeAsset)

    val amountInInput = swapAmountInputMixinFactory.create(
        coroutineScope = viewModelScope,
        assetFlow = assetInFlow.nullOnStart(),
        maxAvailable = { it.transferableInPlanks },
        emptyAssetTitle = R.string.swap_field_asset_from_title
    )

    val amountOutInput = swapAmountInputMixinFactory.create(
        coroutineScope = viewModelScope,
        assetFlow = assetOutFlow.nullOnStart(),
        maxAvailable = { null },
        emptyAssetTitle = R.string.swap_field_asset_to_title
    )

    val feeMixin = feeLoaderMixinFactory.create(
        tokenFlow = feeAssetFlow.map { it.token }
    )

    val rateDetails: Flow<ExtendedLoadingState<String>> = quotingState.map {
        when (it) {
            is QuotingState.NotAvailable, QuotingState.Loading -> ExtendedLoadingState.Loading
            is QuotingState.Loaded -> ExtendedLoadingState.Loaded(formatRate(it.value))
        }
    }
        .shareInBackground()

    val showDetails: Flow<Boolean> = quotingState.map { it !is QuotingState.NotAvailable }
        .shareInBackground()

    val buttonState: Flow<DescriptiveButtonState> = flowOf { DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue)) }

    val swapDirectionFlipped: MutableLiveData<Event<SwapDirection>> = MutableLiveData()

    init {
        initAssetIn()

        handleInputChanges(amountInInput, SwapSettings::assetIn, SwapDirection.SPECIFIED_IN)
        handleInputChanges(amountOutInput, SwapSettings::assetOut, SwapDirection.SPECIFIED_OUT)

        setupQuoting()

        feeMixin.setupFees()
    }

    // TODO sync with iOS team
    fun maxTokens() {
    }

    fun selectPayToken() {
        launch {
            val outAsset = assetOutFlow.firstOrNull()
            swapRouter.selectAssetIn(outAsset?.token?.configuration?.fullId)
        }
    }

    fun selectReceiveToken() {
        launch {
            val inAsset = assetInFlow.firstOrNull()
            swapRouter.selectAssetOut(inAsset?.token?.configuration?.fullId)
        }
    }

    fun confirmButtonClicked() {
        swapRouter.openSwapConfirmation()
    }

    fun rateDetailsClicked() {
        TODO("Not yet implemented")
    }

    fun networkFeeClicked() {
        TODO("Not yet implemented")
    }

    fun flipAssets() = launch {
        val previousSettings = swapSettings.first()
        val newSettings = swapSettingState().flipAssets()

        applyFlipToUi(previousSettings, newSettings)
    }

    fun backClicked() {
        swapRouter.back()
    }

    private fun initAssetIn() {
        launch {
            val chainWithAsset = chainRegistry.chainWithAsset(payload.chainId, payload.assetId)
            swapSettingState().setAssetInUpdatingFee(chainWithAsset.asset, chainWithAsset.chain)
        }
    }

    @OptIn(FlowPreview::class)
    private fun FeeLoaderMixin.Presentation.setupFees() {
        quotingState
            .onEach { if (it is QuotingState.Loading) invalidateFee() }
            .filterIsInstance<QuotingState.Loaded>()
            .debounce(300.milliseconds)
            .onEach { quoteState ->
                val swapArgs = quoteState.quoteArgs.toExecuteArgs(
                    quotedBalance = quoteState.value.quotedBalance,
                    customFeeAsset = quoteState.feeAsset
                )

                loadFeeV2(
                    coroutineScope = viewModelScope,
                    feeConstructor = { swapInteractor.estimateFee(swapArgs).networkFee },
                    onRetryCancelled = {}
                )
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
        val rate = swapQuote.swapRate()

        val assetInUnitFormatted = BigDecimal.ONE.formatTokenAmount(swapQuote.assetIn)
        val rateAmountFormatted = rate.formatTokenAmount(swapQuote.assetOut)

        return "$assetInUnitFormatted ≈ $rateAmountFormatted"
    }

    private fun setupQuoting() {
        swapSettings.mapNotNull { swapSettings ->
            val swapQuoteArgs = swapSettings.toQuoteArgs(
                tokenIn = { assetInFlow.ensureToken(it) },
                tokenOut = { assetOutFlow.ensureToken(it) },
            )

            swapQuoteArgs?.let { it to swapSettings }
        }
            .mapLatest { (quoteArgs, swapSettings) ->
                quotingState.value = QuotingState.Loading

                val quote = swapInteractor.quote(quoteArgs)

                quotingState.value = quote.fold(
                    onSuccess = { QuotingState.Loaded(it, quoteArgs, swapSettings.feeAsset!!) },
                    onFailure = { QuotingState.NotAvailable }
                )

                quote to swapSettings
            }
            .onEach { (quoteResult, swapSettings) -> handleNewQuote(quoteResult, swapSettings) }
            .launchIn(viewModelScope)
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
            SwapQuoteArgs(tokenIn(assetIn!!), tokenOut(assetOut!!), amount!!, swapDirection!!, slippage)
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
            .onEach { state ->
                val asset = chainAsset(swapSettings.first()) ?: return@onEach
                val planks = state.value?.let(asset::planksFromAmount)
                swapSettingState().setAmount(planks, swapDirection)
            }.launchIn(viewModelScope)
    }

    private fun SwapAmountInputMixin.clearInput() {
        inputState.value = InputState(value = "", initiatedByUser = false)
    }

    private fun SwapAmountInputMixin.updateInput(chainAsset: Chain.Asset, planks: Balance) {
        val amount = chainAsset.amountFromPlanks(planks)
        inputState.value = InputState(amountInputFormatter.format(amount), initiatedByUser = false)
    }

    private fun Flow<SwapSettings>.assetFlowOf(extractor: (SwapSettings) -> Chain.Asset?): Flow<Asset> {
        return mapNotNull { extractor(it) }
            .flatMapLatest { assetUseCase.assetFlow(it) }
            .shareInBackground()
    }

    private suspend fun Flow<Asset>.ensureToken(asset: Chain.Asset) = first { it.token.configuration.fullId == asset.fullId }.token

    fun openOptions() {
        swapRouter.openSwapOptions()
    }

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
}
