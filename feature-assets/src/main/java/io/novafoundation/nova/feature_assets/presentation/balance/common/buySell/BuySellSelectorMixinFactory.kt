package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.MultisigRestrictionCheckMixin
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope

class BuySellSelectorMixinFactory(
    private val router: AssetsRouter,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val multisigRestrictionCheckMixin: MultisigRestrictionCheckMixin
) {

    fun create(selectorType: BuySellSelectorMixin.SelectorType, coroutineScope: CoroutineScope): BuySellSelectorMixin {
        return RealBuySellSelectorMixin(
            multisigRestrictionCheckMixin,
            router,
            tradeTokenRegistry,
            chainRegistry,
            resourceManager,
            selectorType,
            coroutineScope
        )
    }
}
