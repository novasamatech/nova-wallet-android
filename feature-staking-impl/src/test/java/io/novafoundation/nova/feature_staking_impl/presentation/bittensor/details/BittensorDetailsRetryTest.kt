package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPriceChartInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDetailsRetryTest {

    private val resources = mock(ResourceManager::class.java)

    private val logger = mock(Logger::class.java)

    private val repository = mock(BittensorPortfolioRepository::class.java)

    private val subnetRepository = mock(BittensorSubnetRepository::class.java)

    private val portfolioInteractor = BittensorPortfolioInteractor(repository, subnetRepository)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val currency = mock(Currency::class.java)

    private val portfolioUpdates = MutableStateFlow(BittensorPortfolio(1L, chain, asset, null, BittensorDataStatus.Fresh))

    private val portfolioLoads = AtomicInteger()

    private val priceLoads = AtomicInteger()

    private var failFirstPortfolio = false

    private var failFirstChart = false

    private var timeoutFirstChart = false

    private val prices = object : CoinPriceRepository {
        override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? = null

        override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
            val attempt = priceLoads.incrementAndGet()
            if (timeoutFirstChart && attempt == 1) withTimeout(1L) { awaitCancellation() }
            check(!failFirstChart || attempt > 1) { "Chart unavailable" }
            return listOf(HistoricalCoinRate(1L, BigDecimal.ONE))
        }
    }

    private val priceChartInteractor = BittensorPriceChartInteractor(prices, logger)

    private val retryController = BittensorRetryController(resources, logger)

    private val router = mock(StakingRouter::class.java)

    private val assetIconProvider = mock(AssetIconProvider::class.java)

    private val externalActions = mock(ExternalActions.Presentation::class.java)

    private val fiatFormatter = mock(FiatFormatter::class.java)

    private val tokenFormatter = mock(TokenFormatter::class.java)

    private val amountFormatter = BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    private val axisFormatterFactory = BittensorChartAxisFormatter.Factory(fiatFormatter, tokenFormatter)

    private lateinit var viewModel: BittensorDetailsViewModel

    @Before
    fun prepare(): Unit = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(resources.getString(anyInt())).thenReturn("Message")
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(token.currency).thenReturn(currency)
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(chainAsset.priceId).thenReturn("native")
        `when`(repository.observeAsset()).thenReturn(flowOf(asset))
        `when`(repository.observePortfolio()).thenReturn(
            flow {
                val attempt = portfolioLoads.incrementAndGet()
                check(!failFirstPortfolio || attempt > 1) { "Portfolio unavailable" }
                emitAll(portfolioUpdates)
            }
        )
        `when`(subnetRepository.subnets()).thenReturn(emptyList())
    }

    @After
    fun cleanUp() {
        if (::viewModel.isInitialized) viewModel.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `portfolio dialog retries only portfolio and clears failed loading`() = runBlocking {
        failFirstPortfolio = true
        createViewModel()

        withTimeout(3_000L) {
            val retry = viewModel.retryEvents.first()
            viewModel.isLoading.first { !it }
            assertNull(viewModel.warning.first())
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }

            retry.onRetry()

            viewModel.canAddStake.first { it }
            assertEquals(2, portfolioLoads.get())
            assertEquals(1, priceLoads.get())
        }
    }

    @Test
    fun `chart dialog retries prices without restarting portfolio`() = runBlocking {
        failFirstChart = true
        createViewModel()

        withTimeout(3_000L) {
            val retry = viewModel.retryEvents.first()
            viewModel.canAddStake.first { it }
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Empty } }

            retry.onRetry()

            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            assertEquals(2, priceLoads.get())
            assertEquals(1, portfolioLoads.get())
        }
    }

    @Test
    fun `stale portfolio continues automatic recovery without retry dialogs`() = runBlocking {
        portfolioUpdates.value = portfolioUpdates.value.copy(dataStatus = BittensorDataStatus.Stale(IllegalStateException("Offline")))
        createViewModel()

        withTimeout(3_000L) {
            viewModel.warning.first { it == resources.getString(R.string.bittensor_positions_stale) }
            assertFalse(viewModel.canAddStake.first())
            portfolioUpdates.value = portfolioUpdates.value.copy(dataStatus = BittensorDataStatus.Fresh)
            viewModel.canAddStake.first { it }
            assertEquals(1, portfolioLoads.get())
            assertNull(withTimeoutOrNull(100L) { viewModel.retryEvents.first() })
        }
    }

    @Test
    fun `chart timeout is recoverable through the same dialog`() = runBlocking {
        timeoutFirstChart = true
        createViewModel()

        withTimeout(3_000L) {
            val retry = viewModel.retryEvents.first()
            retry.onRetry()
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            assertEquals(2, priceLoads.get())
            assertEquals(1, portfolioLoads.get())
        }
    }

    private fun createViewModel() {
        viewModel = BittensorDetailsViewModel(
            bittensorPortfolioInteractor = portfolioInteractor,
            router = router,
            resources = resources,
            amountFormatter = amountFormatter,
            assetIconProvider = assetIconProvider,
            externalActions = externalActions,
            bittensorPriceChartInteractor = priceChartInteractor,
            retryController = retryController,
            chartAxisFormatterFactory = axisFormatterFactory,
            fiatFormatter = fiatFormatter
        )
    }
}
