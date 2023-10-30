package io.novafoundation.nova.feature_buy_api.presentation.mixin

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfManyAwaitable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_buy_api.domain.BuyProvider
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface BuyMixin {

    class IntegrationPayload(val buyProvider: BuyProvider, val integrator: BuyTokenRegistry.Integrator<*>)

    val awaitProviderChoosing: ChooseOneOfManyAwaitable<BuyProvider>

    val integrateWithBuyProviderEvent: LiveData<Event<IntegrationPayload>>

    fun buyEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean>

    fun buyClicked(chainAsset: Chain.Asset)

    interface Presentation : BuyMixin

    interface Factory : MixinFactory<BuyMixin>
}
