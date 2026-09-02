package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeProvider
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class TradeMixinFactory(
    private val buyTokenRegistry: TradeTokenRegistry,
    private val analyticsService: AnalyticsService,
    private val chainRegistry: ChainRegistry,
) : TradeMixin.Factory {

    override fun create(scope: CoroutineScope): TradeMixin.Presentation {
        return TradeProviderMixin(
            buyTokenRegistry = buyTokenRegistry,
            analyticsService = analyticsService,
            chainRegistry = chainRegistry,
            coroutineScope = scope
        )
    }
}

private class TradeProviderMixin(
    private val buyTokenRegistry: TradeTokenRegistry,
    private val analyticsService: AnalyticsService,
    private val chainRegistry: ChainRegistry,
    coroutineScope: CoroutineScope,
) : TradeMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override fun providersFor(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType): List<TradeProvider> {
        return buyTokenRegistry.availableProvidersFor(chainAsset, tradeType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> providerFor(chainAsset: Chain.Asset, tradeFlow: TradeTokenRegistry.TradeType, providerId: String): T {
        val provider = providersFor(chainAsset, tradeFlow)
            .first { it.id == providerId }

        if (provider !is WebViewIntegrationProvider) return provider as T

        return AnalyticsTrackingProvider(provider, analyticsService, chainRegistry, coroutineScope = this) as T
    }
}

/**
 * Wraps a trade provider to report trade analytics:
 * [AnalyticsEvent.BuyInitiated] / [AnalyticsEvent.SellInitiated] are reported when provider flow is about to be opened and
 * [AnalyticsEvent.BuyCompleted] / [AnalyticsEvent.SellCompleted] - when provider reports a successfully finished operation
 */
private class AnalyticsTrackingProvider(
    private val delegate: WebViewIntegrationProvider,
    private val analyticsService: AnalyticsService,
    private val chainRegistry: ChainRegistry,
    private val coroutineScope: CoroutineScope,
) : WebViewIntegrationProvider by delegate {

    override fun createIntegrator(
        chainAsset: Chain.Asset,
        address: String,
        tradeFlow: TradeTokenRegistry.TradeType,
        onCloseListener: OnTradeOperationFinishedListener,
        onSellOrderCreatedListener: OnSellOrderCreatedListener
    ): WebViewIntegrationProvider.Integrator {
        val analyticsSession = TradeAnalyticsSession(
            analyticsService = analyticsService,
            chainRegistry = chainRegistry,
            coroutineScope = coroutineScope,
            providerId = delegate.id,
            chainAsset = chainAsset,
            tradeFlow = tradeFlow,
            delegate = onCloseListener
        )

        return delegate.createIntegrator(
            chainAsset = chainAsset,
            address = address,
            tradeFlow = tradeFlow,
            onCloseListener = analyticsSession,
            onSellOrderCreatedListener = onSellOrderCreatedListener
        )
    }
}

private class TradeAnalyticsSession(
    private val analyticsService: AnalyticsService,
    chainRegistry: ChainRegistry,
    coroutineScope: CoroutineScope,
    private val providerId: String,
    private val chainAsset: Chain.Asset,
    private val tradeFlow: TradeTokenRegistry.TradeType,
    private val delegate: OnTradeOperationFinishedListener,
) : OnTradeOperationFinishedListener {

    // Resolved once when trade is initiated so that completion can be reported without suspending
    @Volatile
    private var networkName: String? = null

    init {
        coroutineScope.launch {
            val network = chainRegistry.getChain(chainAsset.chainId).name
            networkName = network

            analyticsService.track(initiatedEvent(network))
        }
    }

    override fun onTradeOperationFinished(success: Boolean) {
        val network = networkName

        if (success && network != null) {
            analyticsService.track(completedEvent(network))
        }

        delegate.onTradeOperationFinished(success)
    }

    private fun initiatedEvent(network: String): AnalyticsEvent {
        val asset = chainAsset.symbol.value

        return when (tradeFlow) {
            TradeTokenRegistry.TradeType.BUY -> AnalyticsEvent.BuyInitiated(providerId, asset, network)
            TradeTokenRegistry.TradeType.SELL -> AnalyticsEvent.SellInitiated(providerId, asset, network)
        }
    }

    private fun completedEvent(network: String): AnalyticsEvent {
        val asset = chainAsset.symbol.value

        return when (tradeFlow) {
            TradeTokenRegistry.TradeType.BUY -> AnalyticsEvent.BuyCompleted(providerId, asset, network)
            TradeTokenRegistry.TradeType.SELL -> AnalyticsEvent.SellCompleted(providerId, asset, network)
        }
    }
}
