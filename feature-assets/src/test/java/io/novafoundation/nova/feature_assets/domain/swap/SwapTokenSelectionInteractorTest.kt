package io.novafoundation.nova.feature_assets.domain.swap

import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchUseCase
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_swap_api.domain.exchanger.catalogue.ExchangerASwapPair
import io.novafoundation.nova.feature_swap_api.domain.exchanger.catalogue.ExchangerATaoCatalogue
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import io.novafoundation.nova.common.utils.Precision
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger
import java.io.IOException
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class SwapTokenSelectionInteractorTest {
    private val walletRepository = mock(WalletRepository::class.java)
    private val accountRepository = mock(AccountRepository::class.java)
    private val chainRegistry = mock(ChainRegistry::class.java)
    private val swapService = mock(SwapService::class.java)
    private val exchangerATaoCatalogue = mock(ExchangerATaoCatalogue::class.java)
    private val visibility = mock(AssetVisibilityUseCase::class.java)
    private val search = AssetSearchUseCase(walletRepository, accountRepository, chainRegistry, swapService, visibility)
    private val filterFactory = SwapTokenSelectionFilterFactory()

    private val catalogue = SwapTokenSelectionCatalogue(chainRegistry, swapService, exchangerATaoCatalogue, filterFactory)
    private val interactor = SwapTokenSelectionInteractor(walletRepository, search, catalogue, filterFactory)
    private val currency = Currency("USD", "Dollar", "$", Currency.Category.FIAT, true, 0, "usd", true)
    private val pah = chain("pah", "Polkadot Asset Hub")
    private val hydration = chain("hydra", "Hydration")
    private val kusama = chain("ksm", "Kusama")
    private val dot = asset(pah, "DOT", 0, 10)
    private val dotHydra = asset(hydration, "DOT", 0, 0)
    private val usdt = asset(pah, "USDT", 1, 0)
    private val ksm = asset(kusama, "KSM", 0, 100)
    private val all = listOf(dot, dotHydra, usdt, ksm)
    private val compatible = setOf(dot.token.configuration.fullId, dotHydra.token.configuration.fullId, usdt.token.configuration.fullId)

    @Before
    fun setUpCatalogue() = runBlocking<Unit> {
        `when`(exchangerATaoCatalogue.availablePairs()).thenReturn(emptyList())
        `when`(exchangerATaoCatalogue.supportsPair(dot.token.configuration.fullId, usdt.token.configuration.fullId)).thenReturn(false)
    }

    @Test
    fun `incompatible holdings are always in the last section`() {
        val groups = interactor.groups(data(), "", emptySet())
        assertEquals(SwapTokenSection.entries, groups.keys.toList())
        assertEquals(listOf("KSM"), groups.getValue(SwapTokenSection.INCOMPATIBLE).map { it.asset.token.configuration.symbol.value })
        assertEquals("pah", groups.getValue(SwapTokenSection.YOUR_TOKENS).single().chain.id)
        assertTrue(groups.getValue(SwapTokenSection.OTHER_TOKENS).any { it.asset == dotHydra })
    }

    @Test
    fun `empty opposite does not create an incompatible section`() {
        val groups = interactor.groups(data(hasOpposite = false), "", emptySet())
        assertFalse(groups.containsKey(SwapTokenSection.INCOMPATIBLE))
        assertEquals(setOf("DOT", "KSM"), groups.getValue(SwapTokenSection.YOUR_TOKENS).map { it.asset.token.configuration.symbol.value }.toSet())
    }

    @Test
    fun `network filter and search retain incompatible tokens`() {
        val groups = interactor.groups(data(), "ksm", setOf("ksm"))
        assertEquals(listOf(SwapTokenSection.INCOMPATIBLE), groups.keys.toList())
        assertEquals("KSM", groups.values.single().single().asset.token.configuration.symbol.value)
        assertTrue(interactor.groups(data(), "not a token", emptySet()).isEmpty())
    }

    @Test
    fun `network name search returns individual tokens on matching networks`() {
        val groups = interactor.groups(data(), "hydra", emptySet())
        val result = groups.values.single().single()
        assertEquals("DOT", result.asset.token.configuration.symbol.value)
        assertEquals("hydra", result.chain.id)
    }

    @Test
    fun `mixed compatibility splits a token by network`() {
        val groups = interactor.groups(data(compatible = setOf(dot.token.configuration.fullId)), "DOT", emptySet())
        assertEquals("pah", groups.getValue(SwapTokenSection.YOUR_TOKENS).single().chain.id)
        assertEquals("hydra", groups.getValue(SwapTokenSection.INCOMPATIBLE).single().chain.id)
    }

    @Test
    fun `selection rechecks ordered pair for the requested wallet`() = runBlocking<Unit> {
        `when`(pah.assetsById).thenReturn(mapOf(0 to dot.token.configuration, 1 to usdt.token.configuration))
        `when`(chainRegistry.chainsById).thenReturn(MutableStateFlow(mapOf("pah" to pah)))
        val destinations = setOf(usdt.token.configuration.fullId)
        `when`(swapService.availableSwapDirectionsFor(dot.token.configuration, this, 42)).thenReturn(flowOf(destinations))
        assertFalse(interactor.shouldClearOpposite(dot.token.configuration.fullId, usdt.token.configuration.fullId, true, 42, this))
        assertFalse(interactor.shouldClearOpposite(usdt.token.configuration.fullId, dot.token.configuration.fullId, false, 42, this))
        `when`(swapService.availableSwapDirectionsFor(dot.token.configuration, this, 42)).thenReturn(flowOf(emptySet()))
        assertTrue(interactor.shouldClearOpposite(dot.token.configuration.fullId, usdt.token.configuration.fullId, true, 42, this))
    }

    @Test
    fun `selecting either side keeps the opposite when only the provider supports the ordered pair`() = runBlocking<Unit> {
        val bittensor = chain(ChainGeneses.BITTENSOR, "Bittensor")
        val tao = asset(bittensor, "TAO", 0, 0).token.configuration
        val pay = usdt.token.configuration
        val assetsById = mapOf(pay.id to pay)
        val chainsById = mapOf(pah.id to pah, bittensor.id to bittensor)
        `when`(pah.assetsById).thenReturn(assetsById)
        `when`(chainRegistry.chainsById).thenReturn(MutableStateFlow(chainsById))
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah, bittensor)))
        `when`(swapService.availableSwapDirectionsFor(pay, this, 42)).thenReturn(flowOf(emptySet()))
        `when`(exchangerATaoCatalogue.supportsPair(pay.fullId, tao.fullId)).thenReturn(true, true, false)

        assertFalse(interactor.shouldClearOpposite(pay.fullId, tao.fullId, true, 42, this))
        assertFalse(interactor.shouldClearOpposite(tao.fullId, pay.fullId, false, 42, this))
        assertTrue(interactor.shouldClearOpposite(pay.fullId, tao.fullId, true, 42, this))
    }

    @Test
    fun `empty opposite does not request route data`() = runBlocking<Unit> {
        assertFalse(interactor.shouldClearOpposite(dot.token.configuration.fullId, null, true, 42, this))
        verifyNoInteractions(swapService)
    }

    @Test(expected = IOException::class)
    fun `route failure is not treated as an incompatible pair`() = runBlocking<Unit> {
        val config = dot.token.configuration
        `when`(pah.assetsById).thenReturn(mapOf(0 to config))
        `when`(chainRegistry.chainsById).thenReturn(MutableStateFlow(mapOf("pah" to pah)))
        val failure = flow<Set<FullChainAssetId>> { throw IOException("offline") }
        `when`(swapService.availableSwapDirectionsFor(config, this, 42)).thenReturn(failure)
        interactor.shouldClearOpposite(config.fullId, usdt.token.configuration.fullId, true, 42, this)
    }

    @Test
    fun `catalogue includes unsynced tokens and uses the requested wallet`() = runBlocking<Unit> {
        val configurations = listOf(dot.token.configuration, usdt.token.configuration)
        `when`(pah.assets).thenReturn(configurations)
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah)))
        `when`(walletRepository.supportedAssetsFlow(42, configurations)).thenReturn(flowOf(listOf(dot, usdt)))
        `when`(walletRepository.syncedAssetsFlow(42)).thenReturn(flowOf(listOf(dot)))
        val graph = configurations.map { it.fullId }.toSet()
        `when`(swapService.assetsAvailableForSwap(this)).thenReturn(flowOf(graph))
        val result = interactor.observe(42, null, true, this).first()
        assertEquals(listOf(dot, usdt), result.assets)
        verifyNoInteractions(accountRepository)
    }

    @Test
    fun `multiple network filters preserve section ordering without combining token balances`() {
        val groups = interactor.groups(data(), "DOT", setOf("pah", "hydra"))
        assertEquals(listOf(SwapTokenSection.YOUR_TOKENS, SwapTokenSection.OTHER_TOKENS), groups.keys.toList())
        assertEquals(listOf(dot, dotHydra), groups.values.flatten().map { it.asset })
    }

    @Test
    fun `saved network chips lead defaults and never duplicate them`() {
        val polkadot = chain(ChainGeneses.POLKADOT_ASSET_HUB, "Polkadot")
        val astar = chain(ChainGeneses.ASTAR, "Astar")
        val acala = chain("acala", "Acala")
        val result = interactor.networkChips(listOf(polkadot, astar, acala, hydration), setOf("acala", ChainGeneses.ASTAR, "removed"))
        assertEquals(listOf("acala", ChainGeneses.ASTAR, ChainGeneses.POLKADOT_ASSET_HUB), result.map { it.id })
    }

    @Test
    fun `catalogue excludes unsupported routes disabled networks and relay chains`() = runBlocking<Unit> {
        val relay = chain(ChainGeneses.POLKADOT, "Relay")
        val relayDot = asset(relay, "DOT", 0, 100)
        `when`(relay.assets).thenReturn(listOf(relayDot.token.configuration))
        `when`(hydration.connectionState).thenReturn(Chain.ConnectionState.DISABLED)
        `when`(pah.parentId).thenReturn(ChainGeneses.POLKADOT)
        `when`(pah.assets).thenReturn(listOf(dot.token.configuration, usdt.token.configuration))
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah, hydration, relay)))
        val graph = setOf(dot.token.configuration.fullId, dotHydra.token.configuration.fullId, relayDot.token.configuration.fullId)
        `when`(swapService.assetsAvailableForSwap(this)).thenReturn(flowOf(graph))
        `when`(walletRepository.supportedAssetsFlow(42, listOf(dot.token.configuration))).thenReturn(flowOf(listOf(dot)))
        `when`(walletRepository.syncedAssetsFlow(42)).thenReturn(flowOf(listOf(dot, dotHydra, relayDot)))
        val result = interactor.observe(42, null, true, this).first()
        assertEquals(listOf(dot), result.assets)
        assertEquals(listOf(pah), result.chains)
    }

    @Test
    fun `opposite token is omitted only on its exact network`() = runBlocking<Unit> {
        `when`(pah.assets).thenReturn(listOf(dot.token.configuration, usdt.token.configuration))
        `when`(hydration.assets).thenReturn(listOf(dotHydra.token.configuration))
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah, hydration)))
        val configurations = listOf(dot.token.configuration, usdt.token.configuration, dotHydra.token.configuration)
        val graph = configurations.map { it.fullId }.toSet()
        `when`(swapService.assetsAvailableForSwap(this)).thenReturn(flowOf(graph))
        val destinations = setOf(dotHydra.token.configuration.fullId)
        `when`(swapService.availableAssetsForSelection(dot.token.configuration.fullId, true, 42, this)).thenReturn(flowOf(destinations))
        `when`(walletRepository.supportedAssetsFlow(42, configurations)).thenReturn(flowOf(listOf(dot, usdt, dotHydra)))
        `when`(walletRepository.syncedAssetsFlow(42)).thenReturn(flowOf(listOf(dot)))
        val result = interactor.observe(42, dot.token.configuration.fullId, true, this).first()
        assertEquals(listOf(usdt, dotHydra), result.assets)
        assertEquals("DOT", result.oppositeSymbol)
        assertEquals(setOf(dotHydra.token.configuration.fullId), result.compatible)
        verify(swapService).availableAssetsForSelection(dot.token.configuration.fullId, true, 42, this)
    }

    @Test
    fun `paused config network is excluded even when sync and its old routes are enabled`() = runBlocking<Unit> {
        `when`(pah.assets).thenReturn(listOf(dot.token.configuration))
        `when`(hydration.name).thenReturn("Hydration (PAUSED)")
        `when`(hydration.connectionState).thenReturn(Chain.ConnectionState.FULL_SYNC)
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah, hydration)))
        val graph = setOf(dot.token.configuration.fullId, dotHydra.token.configuration.fullId)
        `when`(swapService.assetsAvailableForSwap(this)).thenReturn(flowOf(graph))
        `when`(walletRepository.supportedAssetsFlow(42, listOf(dot.token.configuration))).thenReturn(flowOf(listOf(dot)))
        `when`(walletRepository.syncedAssetsFlow(42)).thenReturn(flowOf(listOf(dot, dotHydra)))
        val result = interactor.observe(42, null, true, this).first()
        assertEquals(listOf(pah), result.chains)
        assertEquals(listOf(dot), result.assets)
    }

    @Test
    fun `synced balances replace catalogue balances and keep updating until observation is cancelled`() = runTest {
        val configurations = listOf(dot.token.configuration, usdt.token.configuration)
        val updatedDot = asset(pah, "DOT", 0, 25)
        val syncedAssets = MutableStateFlow(listOf(dot))
        var balancesObservationClosed = false
        val balancesFlow = syncedAssets.onCompletion { balancesObservationClosed = true }
        val swapAssetIds = configurations.map { it.fullId }.toSet()
        `when`(pah.assets).thenReturn(configurations)
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah)))
        `when`(walletRepository.supportedAssetsFlow(42, configurations)).thenReturn(flowOf(listOf(dot, usdt)))
        `when`(walletRepository.syncedAssetsFlow(42)).thenReturn(balancesFlow)
        `when`(swapService.assetsAvailableForSwap(backgroundScope)).thenReturn(flowOf(swapAssetIds))

        val emissions = mutableListOf<SwapTokenSelectionData>()
        val observation = backgroundScope.launch {
            interactor.observe(42, null, true, backgroundScope).collect { emissions.add(it) }
        }
        runCurrent()
        assertEquals(listOf(dot, usdt), emissions.last().assets)

        syncedAssets.value = listOf(updatedDot)
        runCurrent()
        assertEquals(listOf(updatedDot, usdt), emissions.last().assets)

        observation.cancel()
        runCurrent()
        val emissionCount = emissions.size
        syncedAssets.value = listOf(dot)
        runCurrent()
        assertTrue(balancesObservationClosed)
        assertEquals(emissionCount, emissions.size)
    }

    @Test
    fun `updated swap catalogue replaces wallet subscription and excludes obsolete assets`() = runTest {
        val configurations = listOf(dot.token.configuration, usdt.token.configuration)
        val remainingConfigurations = listOf(usdt.token.configuration)
        val swapAssetIds = MutableStateFlow(configurations.map { it.fullId }.toSet())
        val originalAssets = MutableStateFlow(listOf(dot, usdt))
        var originalObservationClosed = false
        val originalAssetsFlow = originalAssets.onCompletion { originalObservationClosed = true }
        `when`(pah.assets).thenReturn(configurations)
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah)))
        `when`(swapService.assetsAvailableForSwap(backgroundScope)).thenReturn(swapAssetIds)
        `when`(walletRepository.supportedAssetsFlow(42, configurations)).thenReturn(originalAssetsFlow)
        `when`(walletRepository.supportedAssetsFlow(42, remainingConfigurations)).thenReturn(flowOf(listOf(usdt)))
        `when`(walletRepository.syncedAssetsFlow(42)).thenReturn(flowOf(listOf(dot)))

        val emissions = mutableListOf<SwapTokenSelectionData>()
        backgroundScope.launch {
            interactor.observe(42, null, true, backgroundScope).collect { emissions.add(it) }
        }
        runCurrent()
        assertEquals(listOf(dot, usdt), emissions.last().assets)

        swapAssetIds.value = setOf(usdt.token.configuration.fullId)
        runCurrent()
        assertTrue(originalObservationClosed)
        assertEquals(listOf(usdt), emissions.last().assets)
        verify(walletRepository).supportedAssetsFlow(42, remainingConfigurations)

        val emissionCount = emissions.size
        originalAssets.value = listOf(dot)
        runCurrent()
        assertEquals(emissionCount, emissions.size)
        assertEquals(listOf(usdt), emissions.last().assets)
    }

    @Test
    fun `both picker sides merge directed provider pairs with on-chain candidates`() = runTest {
        val bittensor = chain(ChainGeneses.BITTENSOR, "Bittensor")
        val tao = asset(bittensor, "TAO", 0, 0).token.configuration
        val pay = usdt.token.configuration
        val dotConfig = dot.token.configuration
        val chains = MutableStateFlow(listOf(pah, bittensor))
        val pairs = listOf(ExchangerASwapPair(pay, tao), ExchangerASwapPair(pay, tao))
        `when`(exchangerATaoCatalogue.availablePairs()).thenReturn(pairs)
        `when`(chainRegistry.currentChains).thenReturn(chains)
        `when`(pah.assets).thenReturn(listOf(dotConfig, pay))
        `when`(bittensor.assets).thenReturn(listOf(tao))
        val dotId = dotConfig.fullId
        val graph = setOf(dotId, pay.fullId)
        `when`(swapService.assetsAvailableForSwap(backgroundScope)).thenReturn(flowOf(graph))
        `when`(swapService.availableAssetsForSelection(tao.fullId, true, 42, backgroundScope)).thenReturn(flowOf(setOf(dotId)))
        `when`(swapService.availableAssetsForSelection(pay.fullId, false, 42, backgroundScope)).thenReturn(flowOf(emptySet()))
        `when`(swapService.availableAssetsForSelection(tao.fullId, false, 42, backgroundScope)).thenReturn(flowOf(emptySet()))

        val selections = mutableListOf<SwapSelectionTokens>()
        backgroundScope.launch { catalogue.observe(tao.fullId, true, 42, backgroundScope).collect { selections.add(it) } }
        runCurrent()
        assertEquals(listOf(dotConfig, pay), selections.last().assets)
        assertEquals(setOf(dotConfig.fullId, pay.fullId), selections.last().compatible)
        val receive = catalogue.observe(pay.fullId, false, 42, backgroundScope).first { tao in it.assets }
        assertEquals(setOf(tao.fullId), receive.compatible)
        val reverse = catalogue.observe(tao.fullId, false, 42, backgroundScope).first { tao in it.assets }
        assertTrue(reverse.compatible.isEmpty())

        chains.value = listOf(pah)
        runCurrent()
        assertEquals(setOf(dotConfig.fullId), selections.last().compatible)
    }

    @Test
    fun `on-chain rows remain selectable during provider loading and failure`() = runTest {
        val pending = CompletableDeferred<Unit>()
        val provider = object : ExchangerATaoCatalogue {
            override suspend fun supportsPair(pay: FullChainAssetId, receive: FullChainAssetId) = false
            override suspend fun availablePairs(): List<ExchangerASwapPair> {
                pending.await()
                throw IOException("offline")
            }
        }
        val routes = SwapTokenSelectionCatalogue(chainRegistry, swapService, provider, filterFactory)
        val dotConfig = dot.token.configuration
        `when`(chainRegistry.currentChains).thenReturn(MutableStateFlow(listOf(pah)))
        val dotId = dotConfig.fullId
        `when`(pah.assets).thenReturn(listOf(dotConfig))
        `when`(swapService.assetsAvailableForSwap(backgroundScope)).thenReturn(flowOf(setOf(dotId)))
        val selections = mutableListOf<SwapSelectionTokens>()
        backgroundScope.launch { routes.observe(null, true, 42, backgroundScope).collect { selections.add(it) } }
        runCurrent()
        assertEquals(listOf(dotConfig), selections.last().assets)
        pending.complete(Unit)
        runCurrent()
        assertEquals(listOf(dotConfig), selections.last().assets)
    }

    private fun data(compatible: Set<FullChainAssetId> = this.compatible, hasOpposite: Boolean = true) =
        SwapTokenSelectionData(all, listOf(pah, hydration, kusama), compatible, hasOpposite)

    private fun chain(id: String, name: String): Chain = mock(Chain::class.java).also {
        `when`(it.id).thenReturn(id)
        `when`(it.name).thenReturn(name)
    }

    private fun asset(chain: Chain, symbol: String, id: Int, balance: Long): Asset {
        val config = mock(Chain.Asset::class.java)
        val chainId = chain.id
        `when`(config.chainId).thenReturn(chainId)
        `when`(config.id).thenReturn(id)
        `when`(config.symbol).thenReturn(TokenSymbol(symbol))
        `when`(config.precision).thenReturn(Precision(0))
        val token = Token(currency, null, config)
        return Asset(
            token, BigInteger.valueOf(balance), BigInteger.ZERO, BigInteger.ZERO, TransferableMode.REGULAR, EDCountingMode.TOTAL,
            BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
        )
    }
}
