package io.novafoundation.nova.feature_wallet_impl.presentation.getAsset

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.domain.config.GlobalConfig
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.domain.asset.RealAssetGetOptionsUseCase
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class GetAssetReceiveNavigationTest {

    @Test
    fun `first loaded TAO opens the standard Receive destination`() = runBlocking {
        assertReceive(lateAsset = false)
    }

    @Test
    fun `opening before TAO loads waits for eligible options instead of dropping the action`() = runBlocking {
        assertReceive(lateAsset = true)
    }

    @Test
    fun `receive uses the asset shown in the sheet even if the source changes before selection`() = runBlocking {
        assertReceive(lateAsset = false, changeAssetBeforeSelection = true)
    }

    @Test
    fun `watch only TAO keeps the existing Receive policy`() = runBlocking {
        val assets = MutableStateFlow<Chain.Asset?>(asset())
        val fixture = Fixture(this, assets, LightMetaAccount.Type.WATCH_ONLY)
        try {
            fixture.mixin.openAssetOptions()
            val payload = withTimeout(5_000) { fixture.selection.shown.await() }
            assertFalse(GetAssetOption.Receive in payload.availableOptions)
            val updatedOptions = payload.awaitDAppOptions()
            assertFalse(GetAssetOption.Receive in updatedOptions)
            assertTrue(updatedOptions.any { it is GetAssetOption.DApp })
            Mockito.verifyNoInteractions(fixture.router)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `ChangeNOW from real use case opens the configured URL unchanged without address fields`() = runBlocking {
        val assets = MutableStateFlow<Chain.Asset?>(asset())
        val configReady = CompletableDeferred<Unit>()
        val fixture = Fixture(this, assets, LightMetaAccount.Type.SECRETS, configReady)
        try {
            fixture.mixin.openAssetOptions()
            val payload = withTimeout(5_000) { fixture.selection.shown.await() }
            assertEquals(setOf(GetAssetOption.Receive), payload.availableOptions)
            withTimeout(5_000) { fixture.configStarted.await() }
            assertFalse(configReady.isCompleted)

            configReady.complete(Unit)

            val updatedOptions = payload.awaitDAppOptions()
            assertTrue(GetAssetOption.Receive in updatedOptions)
            val changeNow = updatedOptions.filterIsInstance<GetAssetOption.DApp>().single()
            val expectedUrl = fixture.configuredUrl
            assertEquals(expectedUrl, changeNow.url)
            for (field in listOf("recipientAddress", "refundAddress", "backupAddress")) {
                assertFalse("Unexpected address field: $field", changeNow.url.contains(field))
            }

            fixture.selection.selected.complete(changeNow)

            assertEquals(expectedUrl, withTimeout(5_000) { fixture.openedUrl.await() })
            Mockito.verify(fixture.router, Mockito.only()).openDAppBrowser(expectedUrl)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `Receive navigates while optional ChangeNOW config remains suspended`() = runBlocking {
        val assets = MutableStateFlow<Chain.Asset?>(asset())
        val configReady = CompletableDeferred<Unit>()
        val fixture = Fixture(this, assets, LightMetaAccount.Type.SECRETS, configReady)
        try {
            fixture.mixin.openAssetOptions()
            val payload = withTimeout(5_000) { fixture.selection.shown.await() }
            assertEquals(setOf(GetAssetOption.Receive), payload.availableOptions)
            withTimeout(5_000) { fixture.configStarted.await() }

            fixture.selection.selected.complete(GetAssetOption.Receive)

            val destination = withTimeout(5_000) { fixture.received.await() }
            assertEquals(payload.chainAsset.chainId, destination.chainId)
            assertEquals(payload.chainAsset.id, destination.chainAssetId)
            assertFalse(configReady.isCompleted)
            Mockito.verify(fixture.router, Mockito.only()).openReceive(destination)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `open TAO sheet keeps its asset and live options when the source changes or clears`() = runBlocking {
        val dot = asset(ChainGeneses.POLKADOT).copy(symbol = "DOT".asTokenSymbol(), name = "Polkadot")
        for (changedAsset in listOf(dot, null)) {
            val tao = asset()
            val assets = MutableStateFlow<Chain.Asset?>(tao)
            val configReady = CompletableDeferred<Unit>()
            val fixture = Fixture(this, assets, LightMetaAccount.Type.SECRETS, configReady)
            try {
                fixture.mixin.openAssetOptions()
                val payload = withTimeout(5_000) { fixture.selection.shown.await() }
                assertEquals(setOf(GetAssetOption.Receive), payload.availableOptions)
                withTimeout(5_000) { fixture.configStarted.await() }

                assets.value = changedAsset
                yield()
                configReady.complete(Unit)

                val updatedOptions = payload.awaitDAppOptions()
                val changeNow = updatedOptions.filterIsInstance<GetAssetOption.DApp>().single()
                assertEquals(tao, payload.chainAsset)
                assertEquals(setOf(GetAssetOption.Receive, changeNow), updatedOptions)
                assertEquals(fixture.configuredUrl, changeNow.url)
                Mockito.verifyNoInteractions(fixture.router)

                fixture.selection.selected.complete(changeNow)

                assertEquals(fixture.configuredUrl, withTimeout(5_000) { fixture.openedUrl.await() })
                Mockito.verify(fixture.router, Mockito.only()).openDAppBrowser(fixture.configuredUrl)
            } finally {
                coroutineContext.cancelChildren()
            }
        }
    }

    @Test
    fun `cancelling sheet selection cancels the suspended optional config request`() = runBlocking {
        val assets = MutableStateFlow<Chain.Asset?>(asset())
        val configReady = CompletableDeferred<Unit>()
        val fixture = Fixture(this, assets, LightMetaAccount.Type.SECRETS, configReady)
        try {
            fixture.mixin.openAssetOptions()
            val payload = withTimeout(5_000) { fixture.selection.shown.await() }
            assertEquals(setOf(GetAssetOption.Receive), payload.availableOptions)
            withTimeout(5_000) { fixture.configStarted.await() }

            fixture.selection.selected.cancel()

            withTimeout(5_000) { fixture.configCancelled.await() }
            assertTrue(fixture.selection.selected.isCancelled)
            assertFalse(configReady.isCompleted)
            Mockito.verifyNoInteractions(fixture.router)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    private suspend fun GetAssetBottomSheet.Payload.awaitDAppOptions(): Set<GetAssetOption> {
        if (availableOptions.any { it is GetAssetOption.DApp }) return availableOptions

        val updates = requireNotNull(optionUpdates) { "The open sheet must receive optional provider updates" }
        return withTimeout(5_000) { updates.first { options -> options.any { it is GetAssetOption.DApp } } }
    }

    private suspend fun CoroutineScope.assertReceive(lateAsset: Boolean, changeAssetBeforeSelection: Boolean = false) {
        val tao = asset()
        val assets = MutableStateFlow<Chain.Asset?>(tao.takeUnless { lateAsset })
        val initialAssetRead = CompletableDeferred<Unit>()
        val observedAssets = assets.onEach { if (it == null) initialAssetRead.complete(Unit) }
        val fixture = Fixture(this, observedAssets, LightMetaAccount.Type.SECRETS)
        try {
            fixture.mixin.openAssetOptions()
            if (lateAsset) {
                yield()
                withTimeout(5_000) { initialAssetRead.await() }
            }
            assets.value = tao
            val payload = withTimeout(5_000) { fixture.selection.shown.await() }
            assertEquals(tao, payload.chainAsset)
            assertTrue(GetAssetOption.Receive in payload.availableOptions)
            if (changeAssetBeforeSelection) assets.value = asset(ChainGeneses.POLKADOT)
            fixture.selection.selected.complete(GetAssetOption.Receive)

            val destination = withTimeout(5_000) { fixture.received.await() }
            assertEquals(tao.chainId, destination.chainId)
            assertEquals(tao.id, destination.chainAssetId)
            Mockito.verify(fixture.router, Mockito.only()).openReceive(destination)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    private class Fixture(
        scope: CoroutineScope,
        assets: Flow<Chain.Asset?>,
        walletType: LightMetaAccount.Type,
        configReady: CompletableDeferred<Unit>? = null
    ) {

        val router = Mockito.mock(WalletRouter::class.java)

        val received = CompletableDeferred<AssetPayload>()

        val openedUrl = CompletableDeferred<String>()

        val configStarted = CompletableDeferred<Unit>()

        val configCancelled = CompletableDeferred<Unit>()

        val selection = ReceiveSelection()

        val configuredUrl = "https://configured-funding.example/partner/tao?from=usdt&to=tao&link_id=navigation-test"

        private val accounts = Mockito.mock(AccountRepository::class.java)

        private val account = Mockito.mock(MetaAccount::class.java)

        private val transfers = Mockito.mock(CrossChainTransfersUseCase::class.java)

        private val resources = Mockito.mock(ResourceManager::class.java)

        private val selectedAccount = Mockito.mock(SelectedAccountUseCase::class.java)

        private val chains = Mockito.mock(ChainRegistry::class.java)

        private val logger = Mockito.mock(Logger::class.java)

        private val config = GlobalConfig(
            multisigsApiUrl = "https://multisig.example",
            proxyApiUrl = "https://proxy.example",
            multiStakingApiUrl = "https://staking.example",
            infraUrl = null,
            changeNowTaoUrl = configuredUrl
        )

        private val globalConfigDataSource = object : GlobalConfigDataSource {
            override suspend fun getGlobalConfig(): GlobalConfig {
                configStarted.complete(Unit)
                try {
                    configReady?.await()
                    return config
                } catch (error: CancellationException) {
                    configCancelled.complete(Unit)
                    throw error
                }
            }
        }

        private val options = RealAssetGetOptionsUseCase(transfers, accounts, globalConfigDataSource, logger)

        private val actions = Mockito.mock(ActionAwaitableMixin.Factory::class.java)

        private val buttonFilter = flowOf(true)

        val mixin: RealGetAssetOptionsMixin

        init {
            Mockito.`when`(resources.getString(R.string.common_get_token_format, "TAO".asTokenSymbol())).thenReturn("Get TAO")
            Mockito.`when`(resources.getString(R.string.common_get_token_format, "DOT".asTokenSymbol())).thenReturn("Get DOT")
            Mockito.`when`(account.type).thenReturn(walletType)
            Mockito.`when`(accounts.selectedMetaAccountFlow()).thenReturn(flowOf(account))
            Mockito.`when`(transfers.incomingCrossChainDirections(Mockito.any<Flow<Chain.Asset?>>() ?: assets))
                .thenReturn(flowOf(emptyList()))
            Mockito.`when`(actions.create<GetAssetBottomSheet.Payload, GetAssetOption>()).thenReturn(selection)
            Mockito.doAnswer { invocation ->
                received.complete(invocation.getArgument(0))
                Unit
            }.`when`(router).openReceive(Mockito.any(AssetPayload::class.java) ?: AssetPayload("", 0))
            Mockito.doAnswer { invocation ->
                openedUrl.complete(invocation.getArgument(0))
                Unit
            }.`when`(router).openDAppBrowser(Mockito.anyString())
            mixin = RealGetAssetOptionsMixin(
                assets, options, router, resources, selectedAccount, chains, actions, buttonFilter, scope, showWhenNoOptions = true
            )
        }
    }

    private class ReceiveSelection : ActionAwaitableMixin.Presentation<GetAssetBottomSheet.Payload, GetAssetOption> {

        val shown = CompletableDeferred<GetAssetBottomSheet.Payload>()

        val selected = CompletableDeferred<GetAssetOption>()

        override val awaitableActionLiveData: LiveData<Event<ActionAwaitableMixin.Action<GetAssetBottomSheet.Payload, GetAssetOption>>>
            get() = error("Selection is controlled directly by the test")

        override suspend fun awaitAction(payload: GetAssetBottomSheet.Payload): GetAssetOption {
            shown.complete(payload)
            return selected.await()
        }
    }

    private fun asset(chainId: String = ChainGeneses.BITTENSOR) = Chain.Asset(
        icon = null,
        id = 0,
        priceId = null,
        chainId = chainId,
        symbol = "TAO".asTokenSymbol(),
        precision = 9.asPrecision(),
        buyProviders = emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = Chain.Asset.Type.Native,
        source = Chain.Asset.Source.DEFAULT,
        name = "Bittensor"
    )
}
