package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorPortfolioChartInteractorTest {

    private val repository = mock(BittensorPortfolioHistoryRepository::class.java)

    private val calculator = BittensorPortfolioChartCalculator()

    private val accounts = mock(AccountRepository::class.java)

    private val chains = mock(ChainRegistry::class.java)

    private val positionUpdates = BittensorPositionUpdates()

    private val interactor = BittensorPortfolioChartInteractor(
        historyRepository = repository,
        calculator = calculator,
        accounts = accounts,
        chainRegistry = chains,
        positionUpdates = positionUpdates
    )

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val configuration = mock(Chain.Asset::class.java)

    private val currentRate = CoinRateChange(recentRateChange = BigDecimal.ZERO, rate = BigDecimal("410"))

    private val point = BittensorPortfolioHistoryPoint(
        timestamp = 1000,
        tao = BigDecimal("12"),
        usd = BigDecimal("4800"),
        completed = true
    )

    private val status = BittensorPortfolioHistoryStatus(asOf = BigDecimal.ZERO, stale = true)

    private val account = BittensorAccountHistory(
        period = BittensorPortfolioPeriod.ONE_DAY,
        windowStart = 0,
        windowEnd = 3600,
        points = listOf(point),
        status = status
    )

    private val history = BittensorPortfolioHistory(account, fiatHistory = BittensorPortfolioFiatHistory.ReportedUsd)

    /** The grid instant before the point is zero, the point keeps its own timestamp, nothing is appended. */
    private val historicalPoints = listOf(
        BittensorPortfolioValuePoint(timestamp = 0, tao = BigDecimal.ZERO, fiat = BigDecimal.ZERO),
        BittensorPortfolioValuePoint(timestamp = 1000, tao = point.tao, fiat = point.usd)
    )

    init {
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(configuration)
        `when`(configuration.precision).thenReturn(Precision(value = 9))
        `when`(token.coinRate).thenReturn(currentRate)
    }

    @Test
    fun `no holdings still returns historical points`() {
        assertHistory(positions = null)
    }

    @Test
    fun `loading or stale zero holdings cannot add a false drop to zero`() {
        val empty = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())
        val loading = BittensorPortfolio(metaId = 1L, chain, asset, empty, BittensorDataStatus.Loading)
        val stale = loading.copy(dataStatus = BittensorDataStatus.Stale(IllegalStateException("Read failed")))
        assertEquals(historicalPoints, interactor.calculate(loading, history).points)
        assertEquals(historicalPoints, interactor.calculate(stale, history).points)
    }

    @Test
    fun `incomplete total still returns historical points`() {
        val subnetId = BittensorNetuid.Subnet(value = 1)
        val hotkey = AccountIdKey(ByteArray(size = 32))
        val subnet = BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE)
        val positions = BittensorPortfolioPositions(root = emptyList(), subnets = listOf(subnet), subnetPrices = emptyMap())
        assertHistory(positions)
    }

    @Test
    fun `missing current rate still returns history without zero anchor`() {
        `when`(token.coinRate).thenReturn(null)
        val hotkey = AccountIdKey(ByteArray(size = 32))
        val root = BittensorRootPosition(hotkey, BigInteger.TEN.pow(9))
        val positions = BittensorPortfolioPositions(root = listOf(root), subnets = emptyList(), subnetPrices = emptyMap())
        assertHistory(positions)
    }

    @Test
    fun `fresh fully priced holdings append a live anchor in native and fiat units`() {
        val hotkey = AccountIdKey(ByteArray(size = 32))
        val root = BittensorRootPosition(hotkey, BigInteger.TEN.pow(9))
        val positions = BittensorPortfolioPositions(root = listOf(root), subnets = emptyList(), subnetPrices = emptyMap())
        val portfolio = BittensorPortfolio(metaId = 1L, chain, asset, positions, BittensorDataStatus.Fresh)

        val points = interactor.calculate(portfolio, history).points

        assertEquals(historicalPoints, points.dropLast(1))
        val live = points.last()
        assertTrue(live.timestamp > account.windowEnd)
        assertEquals(0, BigDecimal.ONE.compareTo(live.tao))
        assertEquals(0, currentRate.rate.compareTo(live.fiat))
    }

    private fun assertHistory(positions: BittensorPortfolioPositions?) {
        val portfolio = BittensorPortfolio(metaId = 1L, chain, asset, positions, BittensorDataStatus.Fresh)
        val result = interactor.calculate(portfolio, history)
        assertEquals(historicalPoints, result.points)
    }
}
