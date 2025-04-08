package io.novafoundation.nova.feature_buy_api.presentation.mixin

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfManyAwaitable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_buy_api.domain.TradeProvider
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface TradeMixin {

    class IntegrationPayload(val tradeProvider: TradeProvider, val integrator: TradeTokenRegistry.Integrator<*>)

    val awaitProviderChoosing: ChooseOneOfManyAwaitable<TradeProvider>

    val integrateWithBuyProviderEvent: LiveData<Event<IntegrationPayload>>

    fun tradeEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean>

    fun providersFor(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType): List<TradeProvider>

    interface Presentation : TradeMixin

    interface Factory : MixinFactory<TradeMixin>

    suspend fun openProvider(chainAsset: Chain.Asset, provider: TradeTokenRegistry.Provider<*>, tradeType: TradeTokenRegistry.TradeType)
}
