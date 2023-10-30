package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.selectingOneOf
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_buy_api.domain.BuyProvider
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class BuyMixinFactory(
    private val buyTokenRegistry: BuyTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val accountUseCase: SelectedAccountUseCase,
    private val awaitableMixinFactory: ActionAwaitableMixin.Factory
) : BuyMixin.Factory {

    override fun create(scope: CoroutineScope): BuyMixin.Presentation {
        return BuyMixinProvider(
            buyTokenRegistry = buyTokenRegistry,
            chainRegistry = chainRegistry,
            accountUseCase = accountUseCase,
            coroutineScope = scope,
            actionAwaitableMixinFactory = awaitableMixinFactory
        )
    }
}

private class BuyMixinProvider(
    private val buyTokenRegistry: BuyTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val accountUseCase: SelectedAccountUseCase,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    coroutineScope: CoroutineScope,
) : BuyMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val awaitProviderChoosing = actionAwaitableMixinFactory.selectingOneOf<BuyProvider>()

    override val integrateWithBuyProviderEvent = MutableLiveData<Event<BuyMixin.IntegrationPayload>>()

    override fun buyEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return flowOf { buyTokenRegistry.availableProvidersFor(chainAsset).isNotEmpty() }
    }

    override fun buyClicked(chainAsset: Chain.Asset) {
        launch {
            val availableProviders = buyTokenRegistry.availableProvidersFor(chainAsset)

            when {
                availableProviders.isEmpty() -> return@launch
                availableProviders.size == 1 -> openProvider(chainAsset, availableProviders.single())
                else -> {
                    val payload = DynamicListBottomSheet.Payload(availableProviders)
                    val provider = awaitProviderChoosing.awaitAction(payload)

                    openProvider(chainAsset, provider)
                }
            }
        }
    }

    private suspend fun openProvider(chainAsset: Chain.Asset, provider: BuyTokenRegistry.Provider<*>) {
        val chain = chainRegistry.getChain(chainAsset.chainId)
        val address = accountUseCase.getSelectedMetaAccount().requireAddressIn(chain)

        val integrator = provider.createIntegrator(chainAsset, address)

        integrateWithBuyProviderEvent.value = BuyMixin.IntegrationPayload(provider, integrator).event()
    }
}
