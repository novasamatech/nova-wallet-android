package io.novafoundation.nova.feature_swap_impl.presentation.main.chart

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_impl.data.SwapChartPreferences
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import io.novafoundation.nova.feature_wallet_api.domain.price.ChartsInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModelMapper
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.eq
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class SwapChartMixinTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val usd = currency("USD", 0)

    private val eur = currency("EUR", 1)

    private val selectedCurrency = MutableStateFlow(usd)

    private val pay = asset(0, "DOT", "polkadot")

    private val receive = asset(1, "USDT", "tether")

    private val settings = MutableStateFlow(SwapSettings(assetIn = pay, assetOut = receive, slippage = DEFAULT_SLIPPAGE))

    private val payToken = MutableStateFlow(token(pay, usd, "12"))

    private val receiveToken = MutableStateFlow(token(receive, usd, "1"))

    private val currencies = mock(CurrencyRepository::class.java)

    private val tokens = mock(TokenRepository::class.java)

    private val charts = mock(ChartsInteractor::class.java)

    private val preferences = mock(SwapChartPreferences::class.java)

    private val resources = mock(ResourceManager::class.java) { invocation ->
        when (invocation.method.name) {
            "getString" -> invocation.arguments.first().toString()
            "formatDateTime" -> invocation.arguments.first().toString()
            else -> Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val fiatFormatter = mock(FiatFormatter::class.java) { invocation ->
        if (invocation.method.name == "formatFiat") {
            val amount = invocation.getArgument<BigDecimal>(0).stripTrailingZeros().toPlainString()
            val currency = invocation.getArgument<Currency>(1)
            "${currency.code}:$amount"
        } else {
            Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val chartMapper = PriceChartModelMapper(resources)

    private val formatter = SwapChartFormatter(resources, chartMapper, fiatFormatter)

    private val factory = SwapChartMixinFactory(charts, currencies, tokens, preferences, formatter)

    @Before
    fun setUp() {
        `when`(currencies.observeSelectCurrency()).thenReturn(selectedCurrency)
        `when`(preferences.isExpanded()).thenReturn(true)
        `when`(tokens.observeToken(pay)).thenReturn(payToken)
        `when`(tokens.observeToken(receive)).thenReturn(receiveToken)
        `when`(charts.chartFlow(any(), any(), any())).thenAnswer {
            history(it.getArgument(2), if (it.getArgument<String>(0) == "tether") "1" else "10")
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `receive wins even when it is a stablecoin and a flat series is drawn`() = runBlocking<Unit> {
        val mixin = create()
        val plot = mixin.loaded()
        assertEquals(receive, plot.context.asset)
        val points = (plot.selectedChart as PriceChartModel.Chart).priceChart
        assertEquals(2, points.size)
        assertEquals(points.first().price, points.last().price)
        val header = withTimeout(5_000) { mixin.header.first { it.price == "USD:1" && it.change.isNotEmpty() } }
        assertEquals("+0%", header.change)
        verify(charts, never()).chartFlow(eq("polkadot"), any(), any())
    }

    @Test
    fun `empty receive follows pay including a stablecoin`() = runBlocking<Unit> {
        settings.value = SwapSettings(assetIn = receive, slippage = DEFAULT_SLIPPAGE)
        assertEquals(receive, create().loaded().context.asset)
    }

    @Test
    fun `no selected assets shows placeholder without loading history`() = runBlocking<Unit> {
        settings.value = SwapSettings(slippage = DEFAULT_SLIPPAGE)
        val mixin = create()
        val plot = withTimeout(5_000) { mixin.plot.first() }
        assertTrue(plot.selectedChart is PriceChartModel.Empty)
        assertEquals("", withTimeout(5_000) { mixin.header.first() }.price)
        verifyNoInteractions(charts)
    }

    @Test
    fun `live price updates do not reload history or replace the chart being scrubbed`() = runBlocking<Unit> {
        settings.value = SwapSettings(assetIn = pay, slippage = DEFAULT_SLIPPAGE)
        val mixin = create()
        val plot = mixin.loaded()
        val chart = plot.selectedChart as PriceChartModel.Chart
        mixin.selectPoint(PriceChartSelection(chart, chart.priceChart.first()))
        val scrubbed = withTimeout(5_000) { mixin.header.first { it.date != null } }
        assertEquals("USD:10", scrubbed.price)
        payToken.value = token(pay, usd, "14")
        mixin.selectPoint(null)
        val live = withTimeout(5_000) { mixin.header.first { it.price == "USD:14" } }
        assertEquals("+40%", live.change)
        assertSame(plot, mixin.loaded())
        verify(charts).chartFlow("polkadot", usd, PricePeriod.WEEK)
    }

    @Test
    fun `switching token cancels old request and clears old selected point`() = runBlocking<Unit> {
        settings.value = SwapSettings(assetIn = pay, slippage = DEFAULT_SLIPPAGE)
        val mixin = create()
        val old = mixin.loaded().selectedChart as PriceChartModel.Chart
        mixin.selectPoint(PriceChartSelection(old, old.priceChart.first()))
        withTimeout(5_000) { mixin.header.first { it.date != null } }
        settings.value = SwapSettings(assetIn = pay, assetOut = receive, slippage = DEFAULT_SLIPPAGE)
        withTimeout(5_000) { mixin.plot.first { it.context.asset == receive && it.selectedChart is PriceChartModel.Chart } }
        val header = withTimeout(5_000) { mixin.header.first { it.price == "USD:1" } }
        assertEquals(null, header.date)
    }

    @Test
    fun `currency change cancels in flight history and never relabels old fiat`() = runBlocking<Unit> {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        `when`(charts.chartFlow("tether", usd, PricePeriod.WEEK)).thenReturn(
            flow {
                emit(AssetPriceChart(PricePeriod.WEEK, ExtendedLoadingState.Loading))
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        )
        val mixin = create()
        withTimeout(5_000) { started.await() }
        selectedCurrency.value = eur
        withTimeout(5_000) { cancelled.await() }
        val plot = withTimeout(5_000) { mixin.plot.first { it.context.currency == eur && it.selectedChart is PriceChartModel.Chart } }
        assertEquals(eur, plot.context.currency)
        val header = withTimeout(5_000) { mixin.header.first { it.price == "EUR:1" } }
        assertNotNull(header)
        verify(charts).chartFlow("tether", eur, PricePeriod.WEEK)
    }

    @Test
    fun `failed history can retry without modifying the swap form`() = runBlocking<Unit> {
        val attempts = AtomicInteger()
        val form = settings.value
        `when`(charts.chartFlow("tether", usd, PricePeriod.WEEK)).thenAnswer {
            if (attempts.getAndIncrement() == 0) {
                flowOf(AssetPriceChart(PricePeriod.WEEK, ExtendedLoadingState.Error(IllegalStateException("offline"))))
            } else {
                history(PricePeriod.WEEK, "1")
            }
        }
        val mixin = create()
        withTimeout(5_000) { mixin.plot.first { it.failed } }
        mixin.retry()
        assertFalse(mixin.loaded().failed)
        assertEquals(2, attempts.get())
        assertSame(form, settings.value)
    }

    @Test
    fun `empty history is not a failure and still has the live price`() = runBlocking<Unit> {
        `when`(charts.chartFlow("tether", usd, PricePeriod.WEEK)).thenReturn(
            flowOf(AssetPriceChart(PricePeriod.WEEK, ExtendedLoadingState.Loaded(emptyList())))
        )
        val mixin = create()
        val plot = withTimeout(5_000) { mixin.plot.first { it.selectedChart is PriceChartModel.Empty } }
        assertFalse(plot.failed)
        assertEquals("USD:1", withTimeout(5_000) { mixin.header.first { it.price == "USD:1" } }.price)
    }

    @Test
    fun `keyboard temporarily collapses chart without replacing manual preference`() = runBlocking<Unit> {
        val mixin = create()
        assertTrue(withTimeout(5_000) { mixin.expanded.first() })
        mixin.setKeyboardVisible(true)
        assertFalse(withTimeout(5_000) { mixin.expanded.first() })
        mixin.setKeyboardVisible(false)
        assertTrue(withTimeout(5_000) { mixin.expanded.first() })
        verify(preferences, never()).setExpanded(anyBoolean())
        mixin.toggleExpanded()
        assertFalse(withTimeout(5_000) { mixin.expanded.first() })
        verify(preferences).setExpanded(false)
        `when`(preferences.isExpanded()).thenReturn(false)
        assertFalse(withTimeout(5_000) { create().expanded.first() })
    }

    @Test
    fun `external entry starts collapsed without changing global preference`() = runBlocking<Unit> {
        val mixin = create(initiallyCollapsed = true)
        assertFalse(withTimeout(5_000) { mixin.expanded.first() })
        verify(preferences, never()).setExpanded(anyBoolean())
        mixin.onEntry(collapsed = false)
        assertTrue(withTimeout(5_000) { mixin.expanded.first() })
    }

    @Test
    fun `chart loads with a legacy disabled preference and keeps its collapsed state`() = runBlocking<Unit> {
        val storage = mock(Preferences::class.java)
        `when`(storage.booleanFlow("swap.chart.enabled", defaultValue = true)).thenReturn(flowOf(false))
        `when`(storage.getBoolean("swap.chart.enabled", defaultValue = true)).thenReturn(false)
        val chartPreferences = SwapChartPreferences(storage)
        val factory = SwapChartMixinFactory(charts, currencies, tokens, chartPreferences, formatter)

        val mixin = factory.create(settings, initiallyCollapsed = false, scope = scope)

        assertEquals(receive, mixin.loaded().context.asset)
        assertFalse(withTimeout(5_000) { mixin.expanded.first() })
    }

    private fun create(initiallyCollapsed: Boolean = false) = factory.create(settings, initiallyCollapsed, scope)

    private suspend fun SwapChartMixin.loaded(): SwapChartPlotModel = withTimeout(5_000) {
        plot.first { it.selectedChart is PriceChartModel.Chart }
    }

    private fun history(period: PricePeriod, price: String): Flow<AssetPriceChart> = flowOf(
        AssetPriceChart(period, ExtendedLoadingState.Loading),
        AssetPriceChart(
            period,
            ExtendedLoadingState.Loaded(listOf(HistoricalCoinRate(1, price.toBigDecimal()), HistoricalCoinRate(2, price.toBigDecimal())))
        )
    )

    private fun token(asset: Chain.Asset, currency: Currency, price: String) = Token(
        currency,
        CoinRateChange(BigDecimal.ZERO, price.toBigDecimal()),
        asset
    )

    private fun currency(code: String, id: Int) = Currency(code, code, code, Currency.Category.FIAT, true, id, code.lowercase(), true)

    private fun asset(id: Int, symbol: String, priceId: String) = Chain.Asset(
        icon = null,
        id = id,
        priceId = priceId,
        chainId = "chain",
        symbol = TokenSymbol(symbol),
        precision = 12.asPrecision(),
        buyProviders = emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = Chain.Asset.Type.Native,
        source = Chain.Asset.Source.DEFAULT,
        name = symbol
    )
}
