package io.novafoundation.nova.feature_swap_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.UserEditableString
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.CompoundNumberFormatter
import io.novafoundation.nova.common.utils.formatting.DynamicPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.FixedPrecisionFormatter
import io.novafoundation.nova.common.utils.formatting.NumberAbbreviation
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_impl.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.presentation.state.swapSettingsFlow
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView.Model.SwapAssetIcon
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableFormat
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

sealed class QuotingState {

    object Loading: QuotingState()

    object NotAvailable: QuotingState()

    class Loaded(val value: SwapQuote): QuotingState()
}

class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val assetUseCase: ArbitraryAssetUseCase
) : BaseViewModel() {

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

    private val swapSettingState = async {
        swapSettingsStateProvider.getSwapSettingsState(viewModelScope)
    }

    private val swapSettings = swapSettingsStateProvider.swapSettingsFlow(viewModelScope)

    private val quotingState = MutableStateFlow<QuotingState>(QuotingState.NotAvailable)

    private val assetOutFlow = swapSettings
        .mapNotNull { it.assetOut }
        .flatMapLatest { assetUseCase.assetFlow(it) }
        .shareInBackground()

    private val assetInFlow = swapSettings
        .mapNotNull { it.assetIn }
        .flatMapLatest { assetUseCase.assetFlow(it) }
        .shareInBackground()

    private val feeAssetFlow = swapSettings
        .mapNotNull { it.feeAsset }
        .flatMapLatest { assetUseCase.assetFlow(it) }
        .shareInBackground()

    val amountOutInput: MutableStateFlow<UserEditableString> = MutableStateFlow(UserEditableString())
    val amountInInput: MutableStateFlow<UserEditableString> = MutableStateFlow(UserEditableString())

    val amountOutFiat: Flow<String?> = assetOutFlow.combine(amountOutInput, ::mapInputToFiatAmount)
        .shareInBackground()

    val amountInFiat: Flow<String?> = assetInFlow.combine(amountInInput, ::mapInputToFiatAmount)
        .shareInBackground()

    val paymentAsset: Flow<SwapAmountInputView.Model> = swapSettings.map { settings ->
        formatInputAsset(settings.assetIn, R.string.swap_field_asset_from_title)
    }.shareInBackground()

    val receivingAsset: Flow<SwapAmountInputView.Model> = swapSettings.map { settings ->
        formatInputAsset(settings.assetOut, R.string.swap_field_asset_to_title)
    }.shareInBackground()

    val paymentTokenMaxAmount: Flow<String?> = assetInFlow.map { it.transferableFormat() as String? }
        .onStart { emit(null) }
        .shareInBackground()

    val rateDetails: Flow<ExtendedLoadingState<String>> = quotingState.map {
        when(it) {
            is QuotingState.NotAvailable, QuotingState.Loading -> ExtendedLoadingState.Loading
            is QuotingState.Loaded -> ExtendedLoadingState.Loaded(formatRate(it.value))
        }
    }
        .shareInBackground()

    val networkFee: Flow<AmountModel?> = feeAssetFlow.filterNotNull()
        .map { asset -> mapAmountToAmountModel(BigInteger.TEN, asset) }
        .shareInBackground()

    val showDetails: Flow<Boolean> = quotingState.map { it !is QuotingState.NotAvailable }
        .shareInBackground()

    val buttonState: Flow<DescriptiveButtonState> = flowOf { DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue)) }

    val swapDirectionFlipped: MutableLiveData<Event<SwapDirection>> = MutableLiveData()

    init {
        handleInputChanges(amountInInput, SwapSettings::assetIn, SwapDirection.SPECIFIED_IN)
        handleInputChanges(amountOutInput, SwapSettings::assetOut, SwapDirection.SPECIFIED_OUT)

        setupQuoting()
    }

    //TODO sync with iOS team
    fun maxTokens() {

    }

    fun selectPayToken() {
        launch {
            val assets = swapInteractor.availableAssets(viewModelScope)
            val chainAsset = assets[0].token.configuration
            val chain = chainRegistry.getChain(chainAsset.chainId)

            swapSettingState().setAssetInUpdatingFee(chainAsset, chain)
        }
    }

    fun selectReceiveToken() {
        launch {
            val assets = swapInteractor.availableAssets(viewModelScope)
            val chainAsset = assets[1].token.configuration
            swapSettingState().setAssetOut(chainAsset)
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

    private fun applyFlipToUi(previousSettings: SwapSettings, newSettings: SwapSettings) {
        val amount = previousSettings.amount ?: return
        val swapDirection = previousSettings.swapDirection ?: return

        when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> {
                val previousIn = previousSettings.assetIn ?: return
                updateAmountInput(previousIn, amount, amountOutInput)
                clearAmountInput(amountInInput)
            }

            SwapDirection.SPECIFIED_OUT -> {
                val previousOut = previousSettings.assetOut ?: return
                updateAmountInput(previousOut, amount, amountInInput)
                clearAmountInput(amountOutInput)
            }
        }

        swapDirectionFlipped.value = newSettings.swapDirection!!.event()
    }

    private suspend fun formatInputAsset(chainAsset: Chain.Asset?, titleRes: Int): SwapAmountInputView.Model {
        if (chainAsset == null) return defaultInputModel(titleRes)

        val chain = chainRegistry.getChain(chainAsset.chainId)

        return SwapAmountInputView.Model(
            assetIcon = SwapAssetIcon.Chosen(chainAsset.iconUrl),
            title = chainAsset.symbol,
            subtitleIcon = Icon.FromLink(chain.icon),
            subtitle = chain.name,
            showInput = true,
        )
    }

    private fun defaultInputModel(titleRes: Int): SwapAmountInputView.Model {
        return SwapAmountInputView.Model(
            assetIcon = SwapAssetIcon.NotChosen,
            title = resourceManager.getString(titleRes),
            subtitleIcon = null,
            subtitle = resourceManager.getString(R.string.fragment_swap_main_settings_select_token),
            showInput = false,
        )
    }

    private fun formatRate(swapQuote: SwapQuote): String {
        val rate = swapQuote.swapRate()

        val assetInUnitFormatted = BigDecimal.ONE.formatTokenAmount(swapQuote.assetIn)
        val rateAmountFormatted = rate.formatTokenAmount(swapQuote.assetOut)

        return "$assetInUnitFormatted ≈ $rateAmountFormatted"
    }

    // TODO choose amount mixin should let us avoid using Asset in SwapSettings
    private fun mapInputToFiatAmount(asset: Asset?, amount: UserEditableString): String? {
        if (asset == null) return null
        val parsedAmount = amount.value.parseBigDecimalOrNull() ?: return null
        val fiatAmount = asset.token.amountToFiat(parsedAmount)
        return fiatAmount.formatAsCurrency(asset.token.currency)
    }

    private fun updateAmountInput(chainAsset: Chain.Asset, planks: Balance, amountInput: MutableStateFlow<UserEditableString>) {
        val amount = chainAsset.amountFromPlanks(planks)
        amountInput.value = UserEditableString(amountInputFormatter.format(amount), editedByUser = false)
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

                quotingState.value = quote.fold(onSuccess = QuotingState::Loaded, onFailure = { QuotingState.NotAvailable })

                quote to swapSettings
            }
            .onEach { (quoteResult, swapSettings) ->
                quoteResult.onSuccess { quote ->
                    when(swapSettings.swapDirection!!) {
                        SwapDirection.SPECIFIED_IN -> updateAmountInput(quote.assetOut, quote.planksOut, amountOutInput)
                        SwapDirection.SPECIFIED_OUT -> updateAmountInput(quote.assetIn, quote.planksIn, amountInInput)
                    }
                }.onFailure {
                    when (swapSettings.swapDirection!!) {
                        SwapDirection.SPECIFIED_OUT -> clearAmountInput(amountInInput)
                        SwapDirection.SPECIFIED_IN -> clearAmountInput(amountOutInput)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private inline fun SwapSettings.toQuoteArgs(
        tokenIn: (Chain.Asset) -> Token,
        tokenOut: (Chain.Asset) -> Token
    ): SwapQuoteArgs? {
        return if (assetIn != null && assetOut != null && amount != null && swapDirection != null) {
            SwapQuoteArgs(tokenIn(assetIn), tokenOut(assetOut), amount, swapDirection, slippage)
        } else {
            null
        }
    }

    private fun handleInputChanges(
        amountInput: Flow<UserEditableString>,
        chainAsset: (SwapSettings) -> Chain.Asset?,
        swapDirection: SwapDirection
    ) {
        amountInput
            .filter { it.editedByUser }
            .onEach {
                val asset = chainAsset(swapSettings.first()) ?: return@onEach

                val amount = it.value.parseBigDecimalOrNull()
                val planks = amount?.let { asset.planksFromAmount(amount) }
                swapSettingState().setAmount(planks, swapDirection)
            }.launchIn(viewModelScope)
    }

    private fun clearAmountInput(amountInput: MutableStateFlow<UserEditableString>) {
        amountInput.value = UserEditableString(value = "", editedByUser = false)
    }

    private suspend fun Flow<Asset>.ensureToken(asset: Chain.Asset) = first { it.token.configuration.fullId == asset.fullId }.token

    private fun String.parseBigDecimalOrNull() = replace(",", "").toBigDecimalOrNull()
}
