package io.novafoundation.nova.feature_swap_impl.presentation.main.chart

import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_impl.data.SwapChartPreferences
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import io.novafoundation.nova.feature_wallet_api.domain.price.ChartsInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import io.novafoundation.nova.runtime.ext.fullId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class SwapChartMixinFactory @Inject constructor(
    private val chartsInteractor: ChartsInteractor,
    private val currencyRepository: CurrencyRepository,
    private val tokenRepository: TokenRepository,
    private val preferences: SwapChartPreferences,
    private val formatter: SwapChartFormatter
) {

    fun create(settings: Flow<SwapSettings>, initiallyCollapsed: Boolean, scope: CoroutineScope): SwapChartMixin {
        return SwapChartMixin(settings, initiallyCollapsed, chartsInteractor, currencyRepository, tokenRepository, preferences, formatter, scope)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SwapChartMixin(
    settings: Flow<SwapSettings>,
    initiallyCollapsed: Boolean,
    private val chartsInteractor: ChartsInteractor,
    currencyRepository: CurrencyRepository,
    private val tokenRepository: TokenRepository,
    private val preferences: SwapChartPreferences,
    private val formatter: SwapChartFormatter,
    scope: CoroutineScope
) : CoroutineScope by scope {

    private val selectedPeriod = MutableStateFlow(PricePeriod.WEEK)

    private val selectedPoint = MutableStateFlow<PriceChartSelection?>(null)

    private val manualExpanded = MutableStateFlow(!initiallyCollapsed && preferences.isExpanded())

    private val keyboardVisible = MutableStateFlow(false)

    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val asset = settings.map { it.assetOut ?: it.assetIn }
        .distinctUntilChangedBy { it?.fullId }
        .shareInBackground()

    private val token = asset.flatMapLatest(::observeToken)
        .shareInBackground()

    private val context = combine(
        asset,
        currencyRepository.observeSelectCurrency(),
        selectedPeriod
    ) { asset, currency, period -> SwapChartContext(asset, currency, period) }
        .distinctUntilChanged()

    val enabled = preferences.enabled()

    val expanded = combine(
        manualExpanded,
        keyboardVisible
    ) { expanded, keyboardVisible -> expanded && !keyboardVisible }
        .distinctUntilChanged()

    val plot = enabled.flatMapLatest { enabled ->
        if (enabled) context.flatMapLatest(::observePlot) else emptyFlow()
    }
        .shareInBackground()

    val header = combine(
        plot,
        token,
        selectedPoint
    ) { plot, token, selection -> formatter.mapHeader(plot, token, selection) }
        .distinctUntilChanged()
        .shareInBackground()

    fun selectPeriod(index: Int) {
        selectedPoint.value = null
        selectedPeriod.value = swapChartPeriods[index]
    }

    fun selectPoint(selection: PriceChartSelection?) {
        selectedPoint.value = selection
    }

    fun toggleExpanded() {
        val expanded = !manualExpanded.value
        manualExpanded.value = expanded
        preferences.setExpanded(expanded)
    }

    fun setKeyboardVisible(visible: Boolean) {
        keyboardVisible.value = visible
        if (visible) selectedPoint.value = null
    }

    fun onEntry(collapsed: Boolean) {
        manualExpanded.value = !collapsed && preferences.isExpanded()
    }

    fun retry() {
        retryRequests.tryEmit(Unit)
    }

    fun axisFormatter(plot: SwapChartPlotModel) = formatter.axisFormatter(plot.context.currency)

    private fun observeToken(asset: Chain.Asset?): Flow<ExtendedLoadingState<Token?>> {
        if (asset == null) return flowOf(ExtendedLoadingState.Loaded(null))

        return tokenRepository.observeToken(asset)
            .withSafeLoading()
            .onError {
                if (it is CancellationException) throw it
                it.printStackTrace()
            }
    }

    private fun observePlot(context: SwapChartContext): Flow<SwapChartPlotModel> {
        val priceId = context.asset?.priceId
        if (priceId == null) {
            val emptyHistory = AssetPriceChart(context.period, ExtendedLoadingState.Loaded(emptyList()))
            return flowOf(formatter.mapPlot(context, emptyHistory))
        }

        return retryRequests.onStart { emit(Unit) }
            .flatMapLatest { chartsInteractor.chartFlow(priceId, context.currency, context.period) }
            .onEach { history ->
                val error = history.chart as? ExtendedLoadingState.Error
                error?.exception?.printStackTrace()
            }
            .map { formatter.mapPlot(context, it) }
    }
}
