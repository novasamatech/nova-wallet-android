package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.analytics.StakingPortfolioSource
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.RecordingBittensorAnalytics
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaims
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimPreview
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.portfolioHistoryResponse
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorImmediateExecution
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioChartCalculator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioChartInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioRowInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorWalletGate
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.mockito.Mockito.verify
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.ArgumentCaptor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verifyNoMoreInteractions
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDetailsRetryTest {

    private val analyticsService = RecordingBittensorAnalytics()

    private val analytics = analyticsService

    private val accountRepository = mock(AccountRepository::class.java)

    private val metaAccount = mock(MetaAccount::class.java)

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

    private val hotkey = AccountIdKey(ByteArray(32))

    private val rootPosition = BittensorRootPosition(hotkey, BigInteger.TEN.pow(9))

    private val positions = BittensorPortfolioPositions(listOf(rootPosition), emptyList(), emptyMap())

    private val coinRate = mock(CoinRateChange::class.java)

    private val portfolioUpdates = MutableStateFlow(
        BittensorPortfolio(
            1L,
            chain,
            asset,
            positions,
            BittensorDataStatus.Fresh
        )
    )

    private val portfolioLoads = AtomicInteger()

    private val priceLoads = AtomicInteger()

    private var failFirstPortfolio = false

    private var failFirstChart = false

    private var timeoutFirstChart = false

    private var staleHistory = false

    private val requestedPeriods = mutableListOf<String>()

    private val prices = object : CoinPriceRepository {
        override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? = null

        override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
            return listOf(HistoricalCoinRate(1L, BigDecimal.ONE))
        }
    }

    private val historyApi = object : BittensorPortfolioHistoryApi {
        override suspend fun history(url: String, accountSubject: String, period: String): BittensorPortfolioHistoryRemote {
            requestedPeriods += period
            val attempt = priceLoads.incrementAndGet()
            if (timeoutFirstChart && attempt == 1) withTimeout(1L) { awaitCancellation() }
            check(!failFirstChart || attempt > 1) { "Chart unavailable" }
            return portfolioHistoryResponse(period = period, freshness = if (staleHistory) "STALE" else "FRESH")
        }
    }

    private val urls = InfrastructureUrls { "https://infra.example".toHttpUrl() }

    private val historyRepository = BittensorPortfolioHistoryRepository(historyApi, urls, prices)

    private val chartCalculator = BittensorPortfolioChartCalculator()

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val positionUpdates = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates()

    private val priceChartInteractor = BittensorPortfolioChartInteractor(historyRepository, chartCalculator, accountRepository, chainRegistry, positionUpdates)

    private val automaticInteractionGate = mock(AutomaticInteractionGate::class.java)

    private val retryController = BittensorRetryController(resources, logger, automaticInteractionGate)

    private val router = mock(StakingRouter::class.java)

    private val portfolioReopenCommunicator = mock(BittensorPortfolioReopenCommunicator::class.java)

    private val portfolioReopenings = MutableSharedFlow<BittensorPortfolioReopenCommunicator.Response>(extraBufferCapacity = 1)

    private val assetIconProvider = mock(AssetIconProvider::class.java)

    private val rowMapper = mock(BittensorPortfolioRowMapper::class.java)

    private val rowInteractor = mock(BittensorPortfolioRowInteractor::class.java)

    private val rootClaimInteractor = mock(BittensorRootClaimInteractor::class.java)

    private lateinit var rootClaims: MutableStateFlow<BittensorRootClaimState>

    private val externalActions = mock(ExternalActions.Presentation::class.java)

    private val fiatFormatter = mock(FiatFormatter::class.java)

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence =
            "${amount.stripTrailingZeros().toPlainString()} ${token.value}"
    }

    private val amountFormatter = BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    private val axisFormatterFactory = BittensorChartAxisFormatter.Factory(fiatFormatter, tokenFormatter)

    private val actionLauncher = mock(ActionBottomSheetLauncher::class.java)

    private val actionLauncherFactory = object : ActionBottomSheetLauncherFactory {
        override fun create(): ActionBottomSheetLauncher = actionLauncher
    }

    private val signerProvider = mock(SignerProvider::class.java)

    private val immediateExecution = BittensorImmediateExecution(signerProvider)

    private val walletGate = BittensorWalletGate(
        immediateExecution,
        accountRepository,
        chainRegistry,
        resources,
        analytics,
        actionLauncherFactory
    )

    private lateinit var viewModel: BittensorDetailsViewModel

    @Before
    fun prepare(): Unit = runBlocking {
        `when`(metaAccount.id).thenReturn(1L)
        `when`(metaAccount.type).thenReturn(LightMetaAccount.Type.SECRETS)
        `when`(chain.id).thenReturn("chain")
        `when`(chain.name).thenReturn("Bittensor")
        `when`(chainRegistry.getChain("chain")).thenReturn(chain)
        `when`(metaAccount.accountIdIn(chain)).thenReturn(ByteArray(32))
        `when`(accountRepository.getMetaAccount(1L)).thenReturn(metaAccount)
        `when`(accountRepository.getMetaAccount(2L)).thenReturn(metaAccount)
        `when`(currency.code).thenReturn("USD")
        `when`(accountRepository.selectedMetaAccountFlow()).thenReturn(flowOf(metaAccount))
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(resources.getString(anyInt())).thenReturn("Message")
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(token.currency).thenReturn(currency)
        `when`(token.coinRate).thenReturn(coinRate)
        `when`(coinRate.rate).thenReturn(BigDecimal.ONE)
        `when`(chainAsset.precision).thenReturn(Precision(9))
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
        val claims = BittensorRootClaims(emptyList(), BigInteger.ONE)
        val claimState = BittensorRootClaimState(1L, hotkey, chain, asset, claims, true)
        rootClaims = MutableStateFlow(claimState)
        val otherClaims = flowOf(claimState.copy(metaId = 2L))
        `when`(rootClaimInteractor.observe(1L)).thenReturn(rootClaims)
        `when`(rootClaimInteractor.observe(2L)).thenReturn(otherClaims)
        `when`(rowInteractor.weeklyChanges("native", emptySet())).thenReturn(emptyMap())
        `when`(portfolioReopenCommunicator.responseFlow).thenReturn(portfolioReopenings)
    }

    @After
    fun cleanUp() = runBlocking<Unit> {
        if (::viewModel.isInitialized) viewModel.coroutineContext.job.cancelAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `root reward updates change empty portfolio without reopening and do not leak across wallets`() = runBlocking<Unit> {
        portfolioUpdates.value = portfolioUpdates.value.copy(positions = positions.copy(root = emptyList()))
        createViewModel()
        withTimeout(3_000L) {
            viewModel.empty.first { it }
            val preview = BittensorRootClaimPreview(hotkey, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO)
            val rewarded = rootClaims.value.copy(claims = BittensorRootClaims(listOf(preview), BigInteger.ONE))
            rootClaims.value = rewarded
            viewModel.empty.first { !it }
            rootClaims.value = rewarded.copy(claims = BittensorRootClaims(emptyList(), BigInteger.ONE))
            viewModel.empty.first { it }
            rootClaims.value = rewarded
            viewModel.empty.first { !it }
            portfolioUpdates.value = portfolioUpdates.value.copy(metaId = 2L)
            viewModel.empty.first { it }
            rootClaims.value = rewarded.copy(claims = BittensorRootClaims(emptyList(), BigInteger.ONE))
            rootClaims.value = rewarded
            assertNull(withTimeoutOrNull(100L) { viewModel.empty.first { !it } })
        }
    }

    @Test
    fun `portfolio dialog retries only portfolio and clears failed loading`() = runBlocking {
        failFirstPortfolio = true
        createViewModel()

        withTimeout(3_000L) {
            val retry = viewModel.retryEvents.first()
            viewModel.isLoading.first { !it }
            assertNull(viewModel.warning.first())
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Empty } }

            retry.onRetry()

            viewModel.canAddStake.first { it }
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            assertEquals(2, portfolioLoads.get())
            assertEquals(1, priceLoads.get())
        }
    }

    @Test
    fun `portfolio opened is local to screen and not repeated by refreshed data`() = runBlocking {
        createViewModel()
        withTimeout(3_000L) { viewModel.empty.first { !it } }
        portfolioUpdates.value = portfolioUpdates.value.copy(positions = positions.copy(root = emptyList()))
        withTimeout(3_000L) { viewModel.empty.first { it } }
        assertEquals(1, analyticsService.events.count { it.name == "staking_portfolio_opened" })

        viewModel.coroutineContext.job.cancelAndJoin()
        createViewModel()
        withTimeout(3_000L) { viewModel.empty.first { it } }
        assertEquals(2, analyticsService.events.count { it.name == "staking_portfolio_opened" })
    }

    @Test
    fun `returning from a finished flow reports the portfolio opened again from the result`() = runBlocking {
        createViewModel()
        withTimeout(3_000L) { while (portfolioOpenedSources().isEmpty()) delay(10L) }

        portfolioReopenings.emit(BittensorPortfolioReopenCommunicator.Response(StakingPortfolioSource.RESULT))

        withTimeout(3_000L) { while (portfolioOpenedSources().size < 2) delay(10L) }
        assertEquals(listOf("dashboard", "result"), portfolioOpenedSources())
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

    @Test
    fun `add stake requires fresh positions whatever the wallet executes`() = runBlocking<Unit> {
        portfolioUpdates.value = portfolioUpdates.value.copy(supportsImmediateOperations = false)
        createViewModel()

        withTimeout(3_000L) {
            viewModel.canAddStake.first { it }

            portfolioUpdates.value = portfolioUpdates.value.copy(dataStatus = BittensorDataStatus.Stale(IllegalStateException("Offline")))
            viewModel.canAddStake.first { !it }

            portfolioUpdates.value = portfolioUpdates.value.copy(dataStatus = BittensorDataStatus.Fresh)
            viewModel.canAddStake.first { it }
        }
    }

    private fun portfolioOpenedSources() = analyticsService.events.toList().filter { it.name == "staking_portfolio_opened" }.map { it.properties["source"] }

    private fun createViewModel() {
        viewModel = BittensorDetailsViewModel(
            analytics, bittensorPortfolioInteractor = portfolioInteractor,
            router = router,
            portfolioReopenCommunicator = portfolioReopenCommunicator,
            resources = resources,
            amountFormatter = amountFormatter,
            rowMapper = rowMapper,
            externalActions = externalActions,
            bittensorPortfolioChartInteractor = priceChartInteractor,
            retryController = retryController,
            chartAxisFormatterFactory = axisFormatterFactory,
            rowInteractor = rowInteractor,
            rootClaimInteractor = rootClaimInteractor,
            walletGate = walletGate
        )
    }

    @Test
    fun `add position opens the earn landing for the portfolio asset`() = runBlocking {
        createViewModel()
        withTimeout(3_000L) { viewModel.canAddStake.first { it } }

        viewModel.addPosition()

        val captor = ArgumentCaptor.forClass(StartStakingLandingPayload::class.java)
        // Mockito's capture() returns null; the placeholder only satisfies Kotlin's non-null parameter.
        verify(router, timeout(3_000L)).openBittensorStartStaking(captor.capture() ?: StartStakingLandingPayload(AvailableStakingOptionsPayload("", 0, emptyList())))
        val options = captor.value.availableStakingOptions
        assertEquals("chain", options.chainId)
        assertEquals(listOf(Chain.Asset.StakingType.BITTENSOR_ROOT), options.stakingTypes)
        verifyNoMoreInteractions(router)
        val flowOpened = analyticsService.events.single { it.name == "staking_flow_opened" }
        assertEquals("portfolio_add_position", flowOpened.properties["source"])
    }

    @Test
    fun `add position from a proxied wallet is blocked without opening the landing`() = runBlocking {
        `when`(metaAccount.type).thenReturn(LightMetaAccount.Type.PROXIED)
        createViewModel()
        withTimeout(3_000L) { viewModel.canAddStake.first { it } }

        viewModel.addPosition()

        val blocked = withTimeout(3_000L) {
            while (analyticsService.events.none { it.name == "staking_blocked" }) delay(10L)
            analyticsService.events.single { it.name == "staking_blocked" }
        }
        assertEquals("stake", blocked.properties["flow"])
        assertEquals("proxy", blocked.properties["reason"])
        assertEquals("proxied", blocked.properties["wallet_type"])
        assertFalse(blocked.properties.containsKey("target"))
        assertTrue(analyticsService.events.none { it.name == "staking_flow_opened" })
        verifyNoMoreInteractions(router)
    }

    @Test
    fun `balance updates recalculate chart without refetching history`() = runBlocking {
        createViewModel()

        withTimeout(3_000L) {
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            val updatedRoot = rootPosition.copy(stakeInPlanks = BigInteger.TEN.pow(9) * BigInteger.TWO)
            portfolioUpdates.value = portfolioUpdates.value.copy(positions = positions.copy(root = listOf(updatedRoot)))

            viewModel.priceChartModels.first { models ->
                models.filterIsInstance<PriceChartModel.Chart>().any { it.priceChart.last().price.compareTo(BigDecimal(2)) == 0 }
            }
            assertEquals(1, priceLoads.get())
        }
    }

    @Test
    fun `period switch reloads account history without restarting portfolio`() = runBlocking<Unit> {
        createViewModel()
        withTimeout(3_000L) {
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            viewModel.selectChartPeriod(3)
            viewModel.priceChartModels.first { models -> models[3] is PriceChartModel.Chart }
            assertEquals(listOf("THIRTY_DAYS", "NINETY_DAYS"), requestedPeriods)
            assertEquals(1, portfolioLoads.get())
        }
    }

    @Test
    fun `currency change reloads history in the new context`() = runBlocking<Unit> {
        createViewModel()
        withTimeout(3_000L) {
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            val euro = Currency("EUR", "Euro", "€", Currency.Category.FIAT, true, 2, "eur", true)
            val newAsset = mock(Asset::class.java)
            val newToken = mock(Token::class.java)
            `when`(newAsset.token).thenReturn(newToken)
            `when`(newToken.configuration).thenReturn(chainAsset)
            `when`(newToken.currency).thenReturn(euro)
            `when`(newToken.coinRate).thenReturn(coinRate)
            portfolioUpdates.value = portfolioUpdates.value.copy(asset = newAsset)
            viewModel.priceChartModels.first { models -> priceLoads.get() == 2 && models.any { it is PriceChartModel.Chart } }
            assertEquals(1, portfolioLoads.get())
        }
    }

    @Test
    fun `stale backend history displays explicit warning without retry or hiding chart`() = runBlocking<Unit> {
        staleHistory = true
        createViewModel()
        withTimeout(3_000L) {
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            viewModel.warning.first { it == resources.getString(R.string.bittensor_portfolio_history_stale) }
            assertNull(withTimeoutOrNull(100L) { viewModel.retryEvents.first() })
        }
    }

    @Test
    fun `new account requires its own chart request`() = runBlocking<Unit> {
        createViewModel()

        withTimeout(3_000L) {
            viewModel.priceChartModels.first { models -> models.any { it is PriceChartModel.Chart } }
            portfolioUpdates.value = portfolioUpdates.value.copy(metaId = 2L)

            viewModel.priceChartModels.first { models ->
                priceLoads.get() == 2 && models.any { it is PriceChartModel.Chart }
            }
        }
    }
}

