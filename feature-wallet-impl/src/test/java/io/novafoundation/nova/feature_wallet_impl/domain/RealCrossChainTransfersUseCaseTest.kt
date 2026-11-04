package io.novafoundation.nova.feature_wallet_impl.domain

import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunner
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RealCrossChainTransfersUseCaseTest {

    @Test
    fun `overlapping cold destinations keep their own directions while sharing configuration`() = withFixture { fixture ->
        val firstUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        val first = observe(fixture, flowOf(fixture.destinationA), firstUpdates)
        assertEquals(listOf(fixture.balanceA), firstUpdates.next())

        // Keep the first observation alive, as the Get button is when the sheet opens.
        val secondUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        val second = observe(fixture, flowOf(fixture.destinationB), secondUpdates)
        assertEquals(listOf(fixture.balanceB), secondUpdates.next())
        assertEquals(1, fixture.configurationSubscriptions.get())
        fixture.assertSyncCalls(1)

        first.cancelAndJoin()
        fixture.balancesB.value = emptyList()
        assertEquals(emptyList<Asset>(), secondUpdates.next())
        assertEquals(1, fixture.configurationSubscriptions.get())
        fixture.assertSyncCalls(1)

        // Cache release is asynchronous on Dispatchers.Default. Await the actual producer stopping
        // before restarting, so this proves a new lifetime rather than racing cache eviction.
        second.cancelAndJoin()
        fixture.awaitConfigurationStopped()
        val restartedUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        observe(fixture, flowOf(fixture.destinationA), restartedUpdates)
        assertEquals(listOf(fixture.balanceA), restartedUpdates.next())
        assertEquals(2, fixture.configurationSubscriptions.get())
        fixture.assertSyncCalls(2)
    }

    @Test
    fun `loaded cold destination does not reuse an overlapping null destination producer`() = withFixture { fixture ->
        val missingUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        observe(fixture, flowOf(null), missingUpdates)
        assertEquals(emptyList<Asset>(), missingUpdates.next())

        val loadedUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        val loadedDestination = MutableStateFlow<Chain.Asset?>(fixture.destinationA)
        observe(fixture, loadedDestination, loadedUpdates)
        assertEquals(listOf(fixture.balanceA), loadedUpdates.next())
        assertEquals(1, fixture.configurationSubscriptions.get())
        fixture.assertSyncCalls(1)

        loadedDestination.value = fixture.destinationB
        assertEquals(listOf(fixture.balanceB), loadedUpdates.next())
        loadedDestination.value = null
        assertEquals(emptyList<Asset>(), loadedUpdates.next())
        loadedDestination.value = fixture.destinationA
        assertEquals(listOf(fixture.balanceA), loadedUpdates.next())
        assertEquals(1, fixture.configurationSubscriptions.get())
        fixture.assertSyncCalls(1)
    }

    @Test
    fun `wallet switch updates simultaneous observations without a destination or configuration change`() = withFixture { fixture ->
        val buttonUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        val openUpdates = Channel<List<Asset>>(Channel.UNLIMITED)
        observe(fixture, flowOf(fixture.destinationA), buttonUpdates)
        assertEquals(listOf(fixture.balanceA), buttonUpdates.next())
        observe(fixture, flowOf(fixture.destinationA), openUpdates)
        assertEquals(listOf(fixture.balanceA), openUpdates.next())
        fixture.assertSyncCalls(1)

        fixture.selectedAccount.value = fixture.secondAccount
        assertEquals(listOf(fixture.secondWalletBalance), buttonUpdates.next())
        assertEquals(listOf(fixture.secondWalletBalance), openUpdates.next())

        // An old wallet's later balance emission must not reach either observer.
        fixture.balancesA.value = emptyList()
        fixture.secondWalletBalances.value = emptyList()
        assertEquals(emptyList<Asset>(), buttonUpdates.next())
        assertEquals(emptyList<Asset>(), openUpdates.next())

        fixture.selectedAccount.value = fixture.firstAccount
        assertEquals(emptyList<Asset>(), buttonUpdates.next())
        assertEquals(emptyList<Asset>(), openUpdates.next())
        assertEquals(1, fixture.configurationSubscriptions.get())
        fixture.assertSyncCalls(1)
    }

    private fun CoroutineScope.observe(
        fixture: Fixture,
        destination: Flow<Chain.Asset?>,
        updates: Channel<List<Asset>>
    ): Job = launch {
        fixture.useCase.incomingCrossChainDirections(destination).collect { directions ->
            updates.send(directions.map { it.asset })
        }
    }

    // The production cache deliberately uses Dispatchers.Default. Channels establish ordering;
    // timeouts only bound a broken observation, rather than relying on sleeps or runCurrent().
    private suspend fun Channel<List<Asset>>.next(): List<Asset> = withTimeout(5_000) { receive() }

    private fun withFixture(block: suspend CoroutineScope.(Fixture) -> Unit): Unit = runBlocking {
        val fixture = Fixture()
        fixture.setUp()
        try {
            block(fixture)
        } finally {
            coroutineContext[Job]!!.children.toList().forEach { it.cancelAndJoin() }
            fixture.close()
        }
    }

    private class Fixture {

        private val commonModule = CommonModule()

        // Use the actual production factory, including its reference-counted producer cache.
        private val computationalCache = commonModule.provideComputationalCache()

        private val transfers = mock(CrossChainTransfersRepository::class.java)

        private val wallets = mock(WalletRepository::class.java)

        private val chains = mock(ChainRegistry::class.java)

        private val accounts = mock(AccountRepository::class.java)

        private val weigher = mock(CrossChainWeigher::class.java)

        private val transactor = mock(CrossChainTransactor::class.java)

        private val parachains = mock(ParachainInfoRepository::class.java)

        private val dryRunner = mock(XcmTransferDryRunner::class.java)

        private val reserveRegistry = TokenReserveRegistry(parachains, emptyMap(), emptyMap())

        val destinationA = chainAsset("destination", 0)

        val destinationB = chainAsset("destination", 1)

        private val originA = chainAsset("origin-a", 0)

        private val originB = chainAsset("origin-b", 0)

        val balanceA = fundedBalance(originA)

        val balanceB = fundedBalance(originB)

        val secondWalletBalance = fundedBalance(originA)

        val balancesA = MutableStateFlow(listOf(balanceA))

        val balancesB = MutableStateFlow(listOf(balanceB))

        val secondWalletBalances = MutableStateFlow(listOf(secondWalletBalance))

        val firstAccount = mock(MetaAccount::class.java)

        val secondAccount = mock(MetaAccount::class.java)

        val selectedAccount = MutableStateFlow(firstAccount)

        val configurationSubscriptions = AtomicInteger()

        private val configurationStopped = Channel<Unit>(Channel.UNLIMITED)

        private var stoppedConfigurations = 0

        private val syncCalls = AtomicInteger()

        private val syncStarted = Channel<Unit>(Channel.UNLIMITED)

        private var observedSyncCalls = 0

        private val configuration = CrossChainTransfersConfiguration(
            dynamic = DynamicCrossChainTransfersConfiguration(
                reserveRegistry = reserveRegistry,
                customTeleports = emptySet(),
                chains = mapOf(
                    originA.chainId to listOf(route(originA, destinationA)),
                    originB.chainId to listOf(route(originB, destinationB))
                )
            ),
            legacy = LegacyCrossChainTransfersConfiguration(
                assetLocations = emptyMap(),
                feeInstructions = emptyMap(),
                deliveryFeeConfigurations = emptyMap(),
                instructionBaseWeights = emptyMap(),
                chains = emptyMap(),
                reserveRegistry = reserveRegistry
            )
        )

        private val configurations = MutableStateFlow(configuration)

        val useCase = RealCrossChainTransfersUseCase(
            crossChainTransfersRepository = transfers,
            walletRepository = wallets,
            chainRegistry = chains,
            accountRepository = accounts,
            computationalCache = computationalCache,
            crossChainWeigher = weigher,
            crossChainTransactor = transactor,
            parachainInfoRepository = parachains,
            assetTransferDryRunner = dryRunner
        )

        suspend fun setUp() {
            `when`(firstAccount.id).thenReturn(11L)
            `when`(secondAccount.id).thenReturn(22L)
            `when`(accounts.selectedMetaAccountFlow()).thenReturn(selectedAccount)
            // Keep the old implementation runnable so the regression fails on behavior.
            `when`(accounts.getSelectedMetaAccount()).thenAnswer { selectedAccount.value }
            `when`(transfers.syncConfiguration()).thenAnswer {
                syncCalls.incrementAndGet()
                syncStarted.trySend(Unit)
                Unit
            }
            `when`(transfers.configurationFlow()).thenReturn(flow {
                configurationSubscriptions.incrementAndGet()
                try {
                    emitAll(configurations)
                } finally {
                    configurationStopped.trySend(Unit)
                }
            })
            val originChainA = chain(originA)
            val originChainB = chain(originB)
            val chainsById = MutableStateFlow(mapOf(originA.chainId to originChainA, originB.chainId to originChainB))
            `when`(chains.chainsById).thenReturn(chainsById)
            `when`(wallets.assetsFlow(11L, listOf(originA))).thenReturn(balancesA)
            `when`(wallets.assetsFlow(11L, listOf(originB))).thenReturn(balancesB)
            `when`(wallets.assetsFlow(22L, listOf(originA))).thenReturn(secondWalletBalances)
        }

        suspend fun assertSyncCalls(expected: Int) {
            // A shared producer can emit its cached configuration before its sync child runs.
            // Await invocation checkpoints without sleeps or a scheduler tied to the test thread.
            while (observedSyncCalls < expected) {
                withTimeout(5_000) { syncStarted.receive() }
                observedSyncCalls++
            }
            assertEquals(expected, syncCalls.get())
        }

        suspend fun awaitConfigurationStopped() {
            withTimeout(5_000) { configurationStopped.receive() }
            stoppedConfigurations++
        }

        suspend fun close() {
            try {
                repeat(configurationSubscriptions.get() - stoppedConfigurations) {
                    awaitConfigurationStopped()
                }
            } finally {
                (computationalCache as CoroutineScope).cancel()
            }
        }

        private fun chain(asset: Chain.Asset): Chain {
            val chain = mock(Chain::class.java)
            `when`(chain.assetsById).thenReturn(mapOf(asset.id to asset))
            return chain
        }

        private fun fundedBalance(configuration: Chain.Asset): Asset {
            val token = mock(Token::class.java)
            `when`(token.configuration).thenReturn(configuration)
            val asset = mock(Asset::class.java)
            `when`(asset.token).thenReturn(token)
            `when`(asset.transferable).thenReturn(BigDecimal.ONE)
            return asset
        }

        private fun route(origin: Chain.Asset, destination: Chain.Asset) = DynamicCrossChainTransfersConfiguration.AssetTransfers(
            assetId = origin.id,
            destinations = listOf(
                DynamicCrossChainTransfersConfiguration.TransferDestination(
                    fullChainAssetId = destination.fullId,
                    hasDeliveryFee = false,
                    supportsXcmExecute = false
                )
            )
        )

        private fun chainAsset(chainId: String, id: Int) = Chain.Asset(
            icon = null,
            id = id,
            priceId = null,
            chainId = chainId,
            symbol = "TOKEN".asTokenSymbol(),
            precision = 12.asPrecision(),
            buyProviders = emptyMap(),
            sellProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.Native,
            source = Chain.Asset.Source.DEFAULT,
            name = "Token"
        )
    }
}
