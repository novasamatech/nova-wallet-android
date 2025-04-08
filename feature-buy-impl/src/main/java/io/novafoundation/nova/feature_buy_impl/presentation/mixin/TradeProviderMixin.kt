package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeProvider
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

internal class TradeMixinFactory(
    private val buyTokenRegistry: TradeTokenRegistry,
) : TradeMixin.Factory {

    override fun create(scope: CoroutineScope): TradeMixin.Presentation {
        return TradeProviderMixin(
            buyTokenRegistry = buyTokenRegistry,
            coroutineScope = scope
        )
    }
}

private class TradeProviderMixin(
    private val buyTokenRegistry: TradeTokenRegistry,
    coroutineScope: CoroutineScope,
) : TradeMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override fun tradeEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return flowOf { buyTokenRegistry.hasProvider(chainAsset) }
    }

    override fun providersFor(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType): List<TradeProvider> {
        return buyTokenRegistry.availableProvidersFor(chainAsset, tradeType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> providerFor(chainAsset: Chain.Asset, tradeFlow: TradeTokenRegistry.TradeFlow, providerId: String): T {
        return providersFor(chainAsset, tradeFlow)
            .first { it.id == providerId } as T
    }
}
