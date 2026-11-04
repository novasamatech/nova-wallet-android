package io.novafoundation.nova.feature_wallet_impl.presentation.getAsset

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.AssetGetOptionsUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetBottomSheet
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito

class GetAssetDAppNavigationTest {

    private val firstService = GetAssetOption.DApp(
        name = "ExampleSwap",
        url = "https://swap.example/buy?asset=tao",
        supportedTokens = listOf("DOT", "USDC").map { it.asTokenSymbol() }
    )

    private val secondService = GetAssetOption.DApp(
        name = "Another Funding Service",
        url = "https://funding.example/exchange?from=eth&to=tao&ref=wallet",
        supportedTokens = listOf("ETH".asTokenSymbol())
    )

    @Test
    fun `dApp supplied by the use case is offered unchanged and opens its own URL`() = runBlocking {
        assertNavigation(firstService, setOf(firstService))
    }

    @Test
    fun `selecting a second dApp opens its URL without hardcoded service routing`() = runBlocking {
        assertNavigation(secondService, setOf(firstService, secondService))
    }

    private suspend fun CoroutineScope.assertNavigation(selected: GetAssetOption.DApp, options: Set<GetAssetOption>) {
        val asset = Chain.Asset(
            icon = null,
            id = 0,
            priceId = null,
            chainId = ChainGeneses.BITTENSOR,
            symbol = "TAO".asTokenSymbol(),
            precision = 9.asPrecision(),
            buyProviders = emptyMap(),
            sellProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.Native,
            source = Chain.Asset.Source.DEFAULT,
            name = "Bittensor"
        )
        val assetFlow = flowOf(asset)
        val optionsUseCase = Mockito.mock(AssetGetOptionsUseCase::class.java)
        Mockito.`when`(optionsUseCase.observeAssetGetOptionsForSelectedAccount(assetFlow)).thenReturn(flowOf(options))

        val openedUrl = CompletableDeferred<String>()
        val router = Mockito.mock(WalletRouter::class.java)
        Mockito.doAnswer { invocation ->
            openedUrl.complete(invocation.getArgument(0))
            Unit
        }.`when`(router).openDAppBrowser(Mockito.anyString())

        val resourceManager = Mockito.mock(ResourceManager::class.java)
        Mockito.`when`(resourceManager.getString(R.string.common_get_token_format, asset.symbol)).thenReturn("Get TAO")
        val selectedAccountUseCase = Mockito.mock(SelectedAccountUseCase::class.java)
        val chainRegistry = Mockito.mock(ChainRegistry::class.java)
        val selection = DAppSelection(selected)
        val actionFactory = Mockito.mock(ActionAwaitableMixin.Factory::class.java)
        Mockito.`when`(actionFactory.create<GetAssetBottomSheet.Payload, GetAssetOption>()).thenReturn(selection)
        val buttonFilter = flowOf(true)

        val mixin = RealGetAssetOptionsMixin(
            assetFlow = assetFlow,
            assetGetOptionsUseCase = optionsUseCase,
            walletRouter = router,
            resourceManager = resourceManager,
            selectedAccountUseCase = selectedAccountUseCase,
            chainRegistry = chainRegistry,
            actionAwaitableFactory = actionFactory,
            additionalButtonFilter = buttonFilter,
            scope = this,
            showWhenNoOptions = false
        )

        try {
            mixin.openAssetOptions()

            val url = withTimeout(5_000) { openedUrl.await() }
            assertEquals(selected.url, url)
            assertSame(asset, selection.payload.chainAsset)
            assertEquals(options, selection.payload.availableOptions)
            Mockito.verify(router, Mockito.only()).openDAppBrowser(url)
            Mockito.verifyNoInteractions(chainRegistry, selectedAccountUseCase)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    private class DAppSelection(
        private val selected: GetAssetOption.DApp
    ) : ActionAwaitableMixin.Presentation<GetAssetBottomSheet.Payload, GetAssetOption> {

        lateinit var payload: GetAssetBottomSheet.Payload

        override val awaitableActionLiveData: LiveData<Event<ActionAwaitableMixin.Action<GetAssetBottomSheet.Payload, GetAssetOption>>>
            get() = error("This test selects the action without observing the Android UI")

        override suspend fun awaitAction(payload: GetAssetBottomSheet.Payload): GetAssetOption {
            this.payload = payload
            return selected
        }
    }
}
