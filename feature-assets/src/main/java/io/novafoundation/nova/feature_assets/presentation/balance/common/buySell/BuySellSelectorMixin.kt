package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixin.SelectorType
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.MultisigRestrictionCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.isNotMultisig
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface BuySellSelectorMixin {

    sealed interface SelectorType {

        object AllAssets : SelectorType

        class Asset(val chaiId: String, val assetId: Int) : SelectorType
    }

    class SelectorPayload(vararg val items: ListSelectorMixin.Item)

    val multisigRestrictionCheckMixin: MultisigRestrictionCheckMixin

    val tradingEnabledFlow: Flow<Boolean>
    val actionLiveData: LiveData<Event<SelectorPayload>>
    val errorLiveData: MutableLiveData<Event<Pair<String, String>>>

    fun openSelector()
}

class RealBuySellSelectorMixin(
    override val multisigRestrictionCheckMixin: MultisigRestrictionCheckMixin,
    private val router: AssetsRouter,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val selectorType: SelectorType,
    private val coroutineScope: CoroutineScope
) : BuySellSelectorMixin {

    override val tradingEnabledFlow: Flow<Boolean> = flowOf {
        when (selectorType) {
            SelectorType.AllAssets -> true
            is SelectorType.Asset -> {
                val chainAsset = chainRegistry.asset(selectorType.chaiId, selectorType.assetId)
                tradeTokenRegistry.hasProvider(chainAsset)
            }
        }
    }

    override val actionLiveData: MutableLiveData<Event<BuySellSelectorMixin.SelectorPayload>> = MutableLiveData()

    override val errorLiveData: MutableLiveData<Event<Pair<String, String>>> = MutableLiveData()

    override fun openSelector() = coroutineScope.launchUnit {
        val payload = when (selectorType) {
            SelectorType.AllAssets -> openAllAssetsSelector()

            is SelectorType.Asset -> openSpecifiedAssetSelector(selectorType)
        }

        if (payload != null) {
            actionLiveData.value = Event(payload)
        }
    }

    private suspend fun openAllAssetsSelector() = BuySellSelectorMixin.SelectorPayload(
        buyItem(enabled = true) { router.openBuyFlow() },
        sellItem(enabled = multisigRestrictionCheckMixin.isNotMultisig()) { router.openSellFlow() }
    )

    private suspend fun openSpecifiedAssetSelector(selectorType: SelectorType.Asset): BuySellSelectorMixin.SelectorPayload? {
        val chainAsset = chainRegistry.asset(selectorType.chaiId, selectorType.assetId)
        val buyAvailable = tradeTokenRegistry.hasProvider(chainAsset, TradeTokenRegistry.TradeType.BUY)
        val sellAvailable = tradeTokenRegistry.hasProvider(chainAsset, TradeTokenRegistry.TradeType.SELL) &&
            multisigRestrictionCheckMixin.isNotMultisig()

        if (!buyAvailable && !sellAvailable) {
            showErrorMessage(R.string.trade_token_not_supported_title, R.string.trade_token_not_supported_message)
            return null
        }

        return BuySellSelectorMixin.SelectorPayload(
            buyItem(enabled = buyAvailable) { router.openBuyProviders(selectorType.chaiId, selectorType.assetId) },
            sellItem(enabled = sellAvailable) { router.openSellProviders(selectorType.chaiId, selectorType.assetId) }
        )
    }

    private fun buyItem(enabled: Boolean, action: () -> Unit): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_add_circle_outline,
            if (enabled) R.color.icon_primary else R.color.icon_inactive,
            R.string.wallet_asset_buy_tokens,
            if (enabled) R.color.text_primary else R.color.button_text_inactive,
            if (enabled) action else errorAction(R.string.buy_token_not_supported_title, R.string.buy_token_not_supported_message)
        )
    }

    private fun sellItem(enabled: Boolean, action: () -> Unit): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_sell_tokens,
            if (enabled) R.color.icon_primary else R.color.icon_inactive,
            R.string.wallet_asset_sell_tokens,
            if (enabled) R.color.text_primary else R.color.button_text_inactive,
            if (enabled) action else sellErrorAction()
        )
    }

    private fun sellErrorAction(): () -> Unit = {
        coroutineScope.launch {
            if (multisigRestrictionCheckMixin.isMultisig()) {
                multisigRestrictionCheckMixin.showWarning(
                    resourceManager.getString(R.string.multisig_sell_not_supported_title),
                    resourceManager.getString(R.string.multisig_sell_not_supported_message)
                )
            } else {
                showErrorMessage(R.string.sell_token_not_supported_title, R.string.sell_token_not_supported_message)
            }
        }
    }

    private fun errorAction(titleRes: Int, messageRes: Int): () -> Unit = { showErrorMessage(titleRes, messageRes) }

    private fun showErrorMessage(titleRes: Int, messageRes: Int) {
        errorLiveData.value = Event(Pair(resourceManager.getString(titleRes), resourceManager.getString(messageRes)))
    }
}
