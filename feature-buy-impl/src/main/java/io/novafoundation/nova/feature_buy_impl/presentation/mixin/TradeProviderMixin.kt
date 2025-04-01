package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.selectingOneOf
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_buy_api.domain.TradeProvider
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

internal class TradeMixinFactory(
    private val buyTokenRegistry: TradeTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val accountUseCase: SelectedAccountUseCase,
    private val awaitableMixinFactory: ActionAwaitableMixin.Factory
) : TradeMixin.Factory {

    override fun create(scope: CoroutineScope): TradeMixin.Presentation {
        return TradeProviderMixin(
            buyTokenRegistry = buyTokenRegistry,
            chainRegistry = chainRegistry,
            accountUseCase = accountUseCase,
            coroutineScope = scope,
            actionAwaitableMixinFactory = awaitableMixinFactory
        )
    }
}

private class TradeProviderMixin(
    private val buyTokenRegistry: TradeTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val accountUseCase: SelectedAccountUseCase,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    coroutineScope: CoroutineScope,
) : TradeMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val awaitProviderChoosing = actionAwaitableMixinFactory.selectingOneOf<TradeProvider>()

    override val integrateWithBuyProviderEvent = MutableLiveData<Event<TradeMixin.IntegrationPayload>>()

    override fun tradeEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return flowOf { buyTokenRegistry.hasProvider(chainAsset) }
    }

    override fun providersFor(chainAsset: Chain.Asset, tradeFlow: TradeTokenRegistry.TradeFlow): List<TradeProvider> {
        return buyTokenRegistry.availableProvidersFor(chainAsset, tradeFlow)
    }

    override suspend fun openProvider(chainAsset: Chain.Asset, provider: TradeTokenRegistry.Provider<*>, tradeFlow: TradeTokenRegistry.TradeFlow) {
        val chain = chainRegistry.getChain(chainAsset.chainId)
        val address = accountUseCase.getSelectedMetaAccount().requireAddressIn(chain)

        val integrator = provider.createIntegrator(chainAsset, address, tradeFlow)

        integrateWithBuyProviderEvent.value = TradeMixin.IntegrationPayload(provider, integrator).event()
    }
}
