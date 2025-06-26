package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.MultisigRestrictionCheckMixinFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope

class BuySellSelectorMixinFactory(
    private val router: AssetsRouter,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val multisigRestrictionCheckMixinFactory: MultisigRestrictionCheckMixinFactory
) {

    fun create(selectorType: BuySellSelectorMixin.SelectorType, coroutineScope: CoroutineScope): BuySellSelectorMixin {
        return RealBuySellSelectorMixin(
            multisigRestrictionCheckMixinFactory.create(),
            router,
            tradeTokenRegistry,
            chainRegistry,
            resourceManager,
            selectorType,
            coroutineScope
        )
    }
}
