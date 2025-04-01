package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.view.input.selector.DynamicSelectorBottomSheet

fun BaseFragmentMixin<*>.setupBuySellMixin(buySellMixin: BuySellMixin) {
    buySellMixin.actionLiveData.observeEvent { action ->
        DynamicSelectorBottomSheet(
            context = fragment.requireContext(),
            payload = DynamicSelectorBottomSheet.Payload(
                titleRes = null,
                subtitle = null,
                data = action.items.toList()
            ),
            onClicked = { _, item -> item.onClick() },
        ).show()
    }
}
