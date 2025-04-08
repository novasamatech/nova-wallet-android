package io.novafoundation.nova.feature_buy_api.presentation.mixin

import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeProvider
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface TradeMixin {

    fun tradeEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean>

    fun providersFor(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType): List<TradeProvider>

    fun <T> providerFor(chainAsset: Chain.Asset, tradeFlow: TradeTokenRegistry.TradeType, providerId: String): T

    interface Presentation : TradeMixin

    interface Factory : MixinFactory<TradeMixin>
}
