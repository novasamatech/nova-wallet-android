package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BittensorPortfolioHistoryInteractorTest {

    private val hour = 3600L
    private val now = 2_000_000L
    private val windowStart = now - 7 * 24 * hour
    private val logger = mock(Logger::class.java)
    private val currency = mock(Currency::class.java)
    private val updates = BittensorPositionUpdates()
    private val chain = mock(Chain::class.java).also { `when`(it.id).thenReturn("bittensor-chain") }
    private val chainAsset = Chain.Asset(
        icon = null,
        id = 0,
        priceId = "bittensor",
        chainId = "bittensor-chain",
        symbol = TokenSymbol("TAO"),
        precision = 9.asPrecision(),
        buyProviders = emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = Chain.Asset.Type.Native,
        source = Chain.Asset.Source.DEFAULT,
        name = "TAO"
    )

    private var historyRequests = 0
    private var historyFailure: Exception? = null
    private var historyDelay: Duration = Duration.ZERO
    private var historyCancelled = false
    private var requestedGridPeriod: PricePeriod? = null
    private var grid = listOf(HistoricalCoinRate(windowStart, BigDecimal("2")), HistoricalCoinRate(windowStart + hour, BigDecimal("2")))

    private val repository = object : BittensorPortfolioHistoryRepository {
        override suspend fun history(chain: Chain, coldkey: AccountIdKey, period: BittensorPortfolioHistoryPeriod): BittensorPortfolioStakeHistory {
            historyRequests++
            historyFailure?.let { throw it }
            try {
                delay(historyDelay)
            } catch (e: CancellationException) {
                historyCancelled = true
                throw e
            }
            return BittensorPortfolioStakeHistory(
                period,
                windowStart,
                now,
                listOf(
                    BittensorPortfolioStakePoint(windowStart, BigDecimal("10"), BigDecimal.ZERO, completed = true),
                    BittensorPortfolioStakePoint(windowStart + hour, BigDecimal("11"), BigDecimal.ZERO, completed = false)
                )
            )
        }
    }

    private val prices = object : CoinPriceRepository {
        override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? = null

        override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
            requestedGridPeriod = range
            return grid
        }
    }

    private fun portfolio(totalStakePlanks: BigInteger? = BigInteger("15000000000"), rate: BigDecimal? = BigDecimal("4"), metaId: Long = 1L): BittensorPortfolio {
        val token = Token(currency, rate?.let { CoinRateChange(BigDecimal.ZERO, it) }, chainAsset)
        val asset = mock(Asset::class.java).also { `when`(it.token).thenReturn(token) }
        val positions = totalStakePlanks?.let {
            BittensorPortfolioPositions(listOf(BittensorRootPosition(AccountIdKey(ByteArray(32)), it)), emptyList(), emptyMap())
        }
        return BittensorPortfolio(metaId, chain, AccountIdKey(ByteArray(32) { 1 }), asset, positions, BittensorDataStatus.Fresh)
    }

    private fun interactor(fetchTimeout: Duration = 20.seconds) =
        BittensorPortfolioHistoryInteractor(repository, prices, updates, logger, clock = { now }, refreshInterval = 1000.hours, fetchTimeout = fetchTimeout)

    private fun List<BittensorPortfolioValuePoint>.fiat() = map { it.fiatValue.stripTrailingZeros().toPlainString() }

    @Test
    fun `emits loading then the series priced with the fiat grid and the live point`() = runBlocking { withTimeout(10_000L) {
        val states = interactor().observe(MutableStateFlow(portfolio()), BittensorPortfolioHistoryPeriod.SEVEN_DAYS).take(2).toList()

        assertTrue(states.first().series is ExtendedLoadingState.Loading)
        assertEquals(BittensorPortfolioHistoryPeriod.SEVEN_DAYS, states.first().period)
        assertEquals(PricePeriod.MONTH, requestedGridPeriod)

        val series = (states.last().series as ExtendedLoadingState.Loaded).data
        // The open bucket is replaced by the live point: 15 TAO at rate 4
        assertEquals(listOf("20", "60"), series.points.fiat())
        assertEquals(listOf(windowStart, now), series.points.map { it.timestamp })
        assertTrue(series.changeCovered)
    } }

    @Test
    fun `portfolio without a full valuation keeps the open bucket instead of a live point`() = runBlocking { withTimeout(10_000L) {
        val states = interactor().observe(MutableStateFlow(portfolio(totalStakePlanks = null)), BittensorPortfolioHistoryPeriod.SEVEN_DAYS).take(2).toList()
        val series = (states.last().series as ExtendedLoadingState.Loaded).data
        assertEquals(listOf("20", "22"), series.points.fiat())
    } }

    @Test
    fun `repository failure exits loading with an error and logs it`() = runBlocking { withTimeout(10_000L) {
        historyFailure = IllegalStateException("offline")
        val states = interactor().observe(MutableStateFlow(portfolio()), BittensorPortfolioHistoryPeriod.ONE_DAY).take(2).toList()
        val error = states.last().series as ExtendedLoadingState.Error
        assertEquals("offline", error.exception.message)
        verify(logger).log(error.exception)
    } }

    @Test
    fun `cancelling the collector cancels the fetch and logs nothing`() = runBlocking { withTimeout(10_000L) {
        historyDelay = 1000.hours
        val job = launch { interactor().observe(MutableStateFlow(portfolio()), BittensorPortfolioHistoryPeriod.ONE_DAY).collect { } }
        while (historyRequests == 0) yield()

        job.cancelAndJoin()

        assertTrue(historyCancelled)
        verifyNoInteractions(logger)
    } }

    @Test
    fun `fetch timeout exits loading with an error instead of hanging`() = runBlocking { withTimeout(10_000L) {
        historyDelay = 1000.hours
        val states = interactor(fetchTimeout = 50.milliseconds)
            .observe(MutableStateFlow(portfolio()), BittensorPortfolioHistoryPeriod.ONE_DAY).take(2).toList()

        val error = states.last().series as ExtendedLoadingState.Error
        assertTrue(error.exception is TimeoutCancellationException)
        verify(logger).log(error.exception)
    } }

    @Test
    fun `position update refetches the history within the same subscription`() = runBlocking { withTimeout(10_000L) {
        val loaded = interactor().observe(MutableStateFlow(portfolio()), BittensorPortfolioHistoryPeriod.ONE_DAY)
            .filter { it.series is ExtendedLoadingState.Loaded }

        var seen = 0
        loaded.take(2).collect {
            seen++
            if (seen == 1) {
                assertEquals(1, historyRequests)
                updates.refresh()
            }
        }

        assertEquals(2, historyRequests)
    } }

    @Test
    fun `portfolio change recomputes the live point without a refetch`() = runBlocking { withTimeout(10_000L) {
        val source = MutableStateFlow(portfolio())
        val loaded = interactor().observe(source, BittensorPortfolioHistoryPeriod.ONE_DAY).filter { it.series is ExtendedLoadingState.Loaded }

        val states = mutableListOf<BittensorPortfolioHistoryChart>()
        loaded.take(2).collect { chart ->
            states += chart
            if (states.size == 1) source.value = portfolio(totalStakePlanks = BigInteger("20000000000"))
        }

        assertEquals(1, historyRequests)
        assertEquals("60", (states[0].series as ExtendedLoadingState.Loaded).data.points.fiat().last())
        assertEquals("80", (states[1].series as ExtendedLoadingState.Loaded).data.points.fiat().last())
    } }

    @Test
    fun `missing price id fails the chart instead of drawing unpriced points`() = runBlocking { withTimeout(10_000L) {
        val unpriced = chainAsset.copy(priceId = null)
        val token = Token(currency, null, unpriced)
        val asset = mock(Asset::class.java).also { `when`(it.token).thenReturn(token) }
        val portfolio = BittensorPortfolio(1L, chain, AccountIdKey(ByteArray(32)), asset, null, BittensorDataStatus.Fresh)

        val states = interactor().observe(MutableStateFlow(portfolio), BittensorPortfolioHistoryPeriod.ONE_DAY).take(2).toList()
        assertTrue(states.last().series is ExtendedLoadingState.Error)
        assertEquals(0, historyRequests)
    } }
}
