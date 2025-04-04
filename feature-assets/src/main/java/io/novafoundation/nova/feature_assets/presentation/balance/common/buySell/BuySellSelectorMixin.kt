package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixin.Selector

interface BuySellSelectorMixin {

    sealed interface Selector {

        object AllAssets : Selector

        class Asset(val chaiId: String, val assetId: Int) : Selector
    }

    class SelectorPayload(vararg val items: ListSelectorMixin.Item)

    val actionLiveData: LiveData<Event<SelectorPayload>>

    fun openSelector(selector: Selector)
}

class RealBuySellSelectorMixin(
    private val router: AssetsRouter
) : BuySellSelectorMixin {

    override val actionLiveData: MutableLiveData<Event<BuySellSelectorMixin.SelectorPayload>> = MutableLiveData()

    override fun openSelector(selector: Selector) {
        val payload = when (selector) {
            Selector.AllAssets -> BuySellSelectorMixin.SelectorPayload(
                buyItem { router.openBuyFlow() },
                sellItem { router.openSellFlow() }
            )

            is Selector.Asset -> BuySellSelectorMixin.SelectorPayload(
                buyItem { router.openBuyProviders(selector.chaiId, selector.assetId) },
                sellItem { router.openSellProviders(selector.chaiId, selector.assetId) }
            )
        }

        actionLiveData.value = Event(payload)
    }

    private fun buyItem(action: () -> Unit): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_add_circle_outline,
            R.color.icon_primary,
            R.string.wallet_asset_buy_tokens,
            R.color.text_primary,
            action
        )
    }

    private fun sellItem(action: () -> Unit): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_sell_tokens,
            R.color.icon_primary,
            R.string.wallet_asset_sell_tokens,
            R.color.text_primary,
            action
        )
    }
}
