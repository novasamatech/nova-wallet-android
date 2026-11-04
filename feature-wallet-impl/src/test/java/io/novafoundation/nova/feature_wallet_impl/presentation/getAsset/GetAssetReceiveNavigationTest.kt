package io.novafoundation.nova.feature_wallet_impl.presentation.getAsset

import androidx.lifecycle.LiveData
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
            assertTrue(payload.availableOptions.any { it is GetAssetOption.DApp })
            Mockito.verifyNoInteractions(fixture.router)
        } finally {
            coroutineContext.cancelChildren()
        }
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

    private class Fixture(scope: CoroutineScope, assets: Flow<Chain.Asset?>, walletType: LightMetaAccount.Type) {

        val router = Mockito.mock(WalletRouter::class.java)

        val received = CompletableDeferred<AssetPayload>()

        val selection = ReceiveSelection()

        private val accounts = Mockito.mock(AccountRepository::class.java)

        private val account = Mockito.mock(MetaAccount::class.java)

        private val transfers = Mockito.mock(CrossChainTransfersUseCase::class.java)

        private val options = RealAssetGetOptionsUseCase(transfers, accounts)

        private val resources = Mockito.mock(ResourceManager::class.java)

        private val selectedAccount = Mockito.mock(SelectedAccountUseCase::class.java)

        private val chains = Mockito.mock(ChainRegistry::class.java)

        private val actions = Mockito.mock(ActionAwaitableMixin.Factory::class.java)

        private val buttonFilter = flowOf(true)

        val mixin: RealGetAssetOptionsMixin

        init {
            Mockito.`when`(resources.getString(R.string.common_get_token_format, "TAO".asTokenSymbol())).thenReturn("Get TAO")
            Mockito.`when`(account.type).thenReturn(walletType)
            Mockito.`when`(accounts.selectedMetaAccountFlow()).thenReturn(flowOf(account))
            Mockito.`when`(transfers.incomingCrossChainDirections(Mockito.any<Flow<Chain.Asset?>>() ?: assets))
                .thenReturn(flowOf(emptyList()))
            Mockito.`when`(actions.create<GetAssetBottomSheet.Payload, GetAssetOption>()).thenReturn(selection)
            Mockito.doAnswer { invocation ->
                received.complete(invocation.getArgument(0))
                Unit
            }.`when`(router).openReceive(Mockito.any(AssetPayload::class.java) ?: AssetPayload("", 0))
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
