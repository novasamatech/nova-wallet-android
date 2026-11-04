package io.novafoundation.nova.feature_wallet_impl.domain.asset

import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.domain.config.GlobalConfig
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.IncomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class RealAssetGetOptionsUseCaseTest {

    private val crossChainTransfers = mock(CrossChainTransfersUseCase::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val selectedAccount = mock(MetaAccount::class.java)

    private val logger = mock(Logger::class.java)

    private val configuredUrl = "https://Funding.Example:443/custom/tao?from=dot&to=tao&link_id=partner-test&campaign=wallet%20funding#exchange"

    private var configRequests = 0

    private var loadGlobalConfig: suspend () -> GlobalConfig = { config() }

    private val globalConfigDataSource = object : GlobalConfigDataSource {
        override suspend fun getGlobalConfig(): GlobalConfig {
            configRequests++
            return loadGlobalConfig()
        }
    }

    private val useCase = RealAssetGetOptionsUseCase(crossChainTransfers, accounts, globalConfigDataSource, logger)

    private val changeNow = GetAssetOption.DApp(
        name = "ChangeNOW",
        url = configuredUrl,
        supportedTokens = listOf("DOT", "USDT", "USDC").map { it.asTokenSymbol() }
    )

    @Before
    fun setUp() {
        `when`(selectedAccount.type).thenReturn(LightMetaAccount.Type.SECRETS)
        `when`(accounts.selectedMetaAccountFlow()).thenReturn(flowOf(selectedAccount))
    }

    @Test
    fun `native TAO offers configured HTTPS URL unchanged without recipient refund or backup fields`() = runBlocking {
        val result = options(asset())

        assertEquals(setOf(GetAssetOption.Receive, changeNow), result)
        val url = result.filterIsInstance<GetAssetOption.DApp>().single().url
        assertEquals(configuredUrl, url)
        for (field in listOf("recipientAddress", "refundAddress", "backupAddress")) {
            assertFalse("Unexpected address field: $field", url.contains(field))
        }
        assertEquals(1, configRequests)
    }

    @Test
    fun `configured query parameters keep their values order duplicates and encoding`() = runBlocking {
        val url = "https://funding.example:8443/custom/tao?tag=one&tag=two&campaign=wallet%20funding&value=a%2fb#exchange"
        loadGlobalConfig = { config(url) }

        assertEquals(setOf(GetAssetOption.Receive, changeNow.copy(url = url)), options(asset()))
    }

    @Test
    fun `absent configured URL omits only ChangeNOW`() = runBlocking {
        loadGlobalConfig = { config(null) }

        assertOtherOptionsRemain()
    }

    @Test
    fun `empty configured URL omits only ChangeNOW`() = runBlocking {
        loadGlobalConfig = { config("") }

        assertOtherOptionsRemain()
    }

    @Test
    fun `malformed configured URL omits only ChangeNOW`() = runBlocking {
        for (url in listOf("not a URL", "https://", "https://funding.example:invalid/tao")) {
            loadGlobalConfig = { config(url) }

            assertOtherOptionsRemain()
        }
    }

    @Test
    fun `non HTTPS configured URLs omit only ChangeNOW`() = runBlocking {
        for (url in listOf("http://funding.example/tao", "javascript:alert(1)", "ftp://funding.example/tao")) {
            loadGlobalConfig = { config(url) }

            assertOtherOptionsRemain()
        }
    }

    @Test
    fun `config network failure omits only ChangeNOW`() = runBlocking {
        val failure = IOException("Config unavailable")
        loadGlobalConfig = { throw failure }

        assertOtherOptionsRemain()
        verify(logger).log(failure)
    }

    @Test
    fun `TAO symbol on another chain does not offer ChangeNOW`() = runBlocking {
        val unrelatedTao = asset(chainId = ChainGeneses.POLKADOT, symbol = "TAO")

        assertEquals(setOf(GetAssetOption.Receive), options(unrelatedTao))
        assertEquals(0, configRequests)
    }

    @Test
    fun `non native asset on Bittensor does not offer ChangeNOW`() = runBlocking {
        val token = asset(type = Chain.Asset.Type.EvmErc20("0x0000000000000000000000000000000000000001"))

        assertEquals(setOf(GetAssetOption.Receive), options(token))
        assertEquals(0, configRequests)
    }

    @Test
    fun `missing asset has no funding options`() = runBlocking {
        assertEquals(emptySet<GetAssetOption>(), options(null))
        assertEquals(0, configRequests)
    }

    @Test
    fun `ordinary asset keeps receive cross chain and provider buy options`() = runBlocking {
        val ordinary = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT", buyAvailable = true)
        val incomingDirection = mock(IncomingDirection::class.java)

        assertEquals(
            setOf(GetAssetOption.Receive, GetAssetOption.CrossChain, GetAssetOption.Buy),
            options(ordinary, listOf(incomingDirection))
        )
        assertEquals(0, configRequests)
    }

    @Test
    fun `watch only account keeps existing receive restriction for ordinary assets`() = runBlocking {
        `when`(selectedAccount.type).thenReturn(LightMetaAccount.Type.WATCH_ONLY)
        val ordinary = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT", buyAvailable = true)
        val incomingDirection = mock(IncomingDirection::class.java)

        assertEquals(setOf(GetAssetOption.CrossChain, GetAssetOption.Buy), options(ordinary, listOf(incomingDirection)))
    }

    @Test
    fun `watch only native TAO offers configured ChangeNOW without Receive`() = runBlocking {
        `when`(selectedAccount.type).thenReturn(LightMetaAccount.Type.WATCH_ONLY)

        assertEquals(setOf(changeNow), options(asset()))
    }

    @Test
    fun `switching away from native TAO removes ChangeNOW from an active observation`() = runBlocking {
        val selectedAsset = MutableStateFlow<Chain.Asset?>(asset())
        val updates = Channel<Set<GetAssetOption>>(Channel.UNLIMITED)
        val observation = observeOptions(selectedAsset, updates)
        try {
            updates.awaitOptions { changeNow in it }

            selectedAsset.value = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT")
            val changed = updates.awaitOptions { it == setOf(GetAssetOption.Receive) }

            assertEquals(setOf(GetAssetOption.Receive), changed)
        } finally {
            observation.cancelAndJoin()
            updates.close()
        }
    }

    @Test
    fun `Receive remains available while config loads and ChangeNOW appears when it completes`() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val configResult = CompletableDeferred<GlobalConfig>()
        loadGlobalConfig = {
            requestStarted.complete(Unit)
            configResult.await()
        }
        val updates = Channel<Set<GetAssetOption>>(Channel.UNLIMITED)
        val observation = observeOptions(flowOf(asset()), updates)
        try {
            withTimeout(5_000) { requestStarted.await() }

            assertEquals(setOf(GetAssetOption.Receive), withTimeout(5_000) { updates.receive() })
            assertFalse(configResult.isCompleted)

            configResult.complete(config())

            assertEquals(setOf(GetAssetOption.Receive, changeNow), updates.awaitOptions { changeNow in it })
            assertEquals(1, configRequests)
        } finally {
            observation.cancelAndJoin()
            updates.close()
        }
    }

    @Test
    fun `asset change cancels pending config and keeps ordinary funding options`() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val requestCancelled = CompletableDeferred<Unit>()
        loadGlobalConfig = {
            requestStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                requestCancelled.complete(Unit)
            }
        }
        val selectedAsset = MutableStateFlow<Chain.Asset?>(asset())
        val updates = Channel<Set<GetAssetOption>>(Channel.UNLIMITED)
        val observation = observeOptions(selectedAsset, updates)
        try {
            withTimeout(5_000) { requestStarted.await() }

            selectedAsset.value = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT", buyAvailable = true)

            withTimeout(5_000) { requestCancelled.await() }
            assertEquals(
                setOf(GetAssetOption.Receive, GetAssetOption.Buy),
                updates.awaitOptions { GetAssetOption.Buy in it }
            )
            verifyNoInteractions(logger)
        } finally {
            observation.cancelAndJoin()
            updates.close()
        }
    }

    @Test
    fun `cancelling observation cancels config without emitting another fallback or logging failure`() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val requestCancelled = CompletableDeferred<Unit>()
        loadGlobalConfig = {
            requestStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                requestCancelled.complete(Unit)
            }
        }
        val updates = Channel<Set<GetAssetOption>>(Channel.UNLIMITED)
        val observation = observeOptions(flowOf(asset()), updates)
        try {
            withTimeout(5_000) { requestStarted.await() }
            assertEquals(setOf(GetAssetOption.Receive), withTimeout(5_000) { updates.receive() })

            observation.cancelAndJoin()

            assertTrue(requestCancelled.isCompleted)
            assertTrue(updates.tryReceive().isFailure)
            verifyNoInteractions(logger)
        } finally {
            observation.cancelAndJoin()
            updates.close()
        }
    }

    private fun CoroutineScope.observeOptions(assets: Flow<Chain.Asset?>, updates: Channel<Set<GetAssetOption>>) = launch {
        `when`(crossChainTransfers.incomingCrossChainDirections(assets)).thenReturn(flowOf(emptyList()))
        useCase.observeAssetGetOptionsForSelectedAccount(assets).collect { updates.send(it) }
    }

    private suspend fun Channel<Set<GetAssetOption>>.awaitOptions(predicate: (Set<GetAssetOption>) -> Boolean): Set<GetAssetOption> {
        return withTimeout(5_000) {
            var options = receive()
            while (!predicate(options)) options = receive()
            options
        }
    }

    private suspend fun assertOtherOptionsRemain() {
        val direction = mock(IncomingDirection::class.java)
        assertEquals(
            setOf(GetAssetOption.Receive, GetAssetOption.CrossChain, GetAssetOption.Buy),
            options(asset(buyAvailable = true), listOf(direction))
        )
    }

    private fun config(url: String? = configuredUrl) = GlobalConfig(
        multisigsApiUrl = "https://multisig.example",
        proxyApiUrl = "https://proxy.example",
        multiStakingApiUrl = "https://staking.example",
        infraUrl = null,
        changeNowTaoUrl = url
    )

    private suspend fun options(asset: Chain.Asset?, directions: List<IncomingDirection> = emptyList()): Set<GetAssetOption> {
        val assetFlow = flowOf(asset)
        val incomingDirections = flowOf(directions)
        `when`(crossChainTransfers.incomingCrossChainDirections(assetFlow)).thenReturn(incomingDirections)
        return withTimeout(5_000) { useCase.observeAssetGetOptionsForSelectedAccount(assetFlow).last() }
    }

    private fun asset(
        chainId: String = ChainGeneses.BITTENSOR,
        symbol: String = "TAO",
        type: Chain.Asset.Type = Chain.Asset.Type.Native,
        buyAvailable: Boolean = false
    ) = Chain.Asset(
        icon = null,
        id = 0,
        priceId = null,
        chainId = chainId,
        symbol = symbol.asTokenSymbol(),
        precision = 9.asPrecision(),
        buyProviders = if (buyAvailable) mapOf("provider" to emptyMap()) else emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = type,
        source = Chain.Asset.Source.DEFAULT,
        name = symbol
    )
}
