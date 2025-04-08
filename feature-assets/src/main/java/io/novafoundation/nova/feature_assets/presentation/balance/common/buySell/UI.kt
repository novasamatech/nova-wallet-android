package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import android.annotation.SuppressLint
import android.widget.TextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.utils.ViewClickGestureDetector
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.common.view.input.selector.DynamicSelectorBottomSheet

fun BaseFragmentMixin<*>.setupBuySellSelectorMixin(
    buySellSelectorMixin: BuySellSelectorMixin
) {
    buySellSelectorMixin.actionLiveData.observeEvent { action ->
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

    buySellSelectorMixin.errorLiveData.observeEvent {
        dialog(providedContext) {
            setTitle(it.first)
            setMessage(it.second)
            setPositiveButton(R.string.common_got_it) { _, _ -> }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
fun BaseFragmentMixin<*>.setupButSellActionButton(
    buySellSelectorMixin: BuySellSelectorMixin,
    actionButton: TextView
) {
    val clickDetector = ViewClickGestureDetector(actionButton)
    actionButton.setOnTouchListener { v, event ->
        clickDetector.onTouchEvent(event)
    }
    actionButton.setOnClickListener { buySellSelectorMixin.openSelector() }

    buySellSelectorMixin.tradingEnabledFlow.observe {
        if (it) {
            actionButton.setTextColorRes(R.color.actions_color)
            actionButton.setCompoundDrawableTint(actionButton.context.getColor(R.color.actions_color))
        } else {
            actionButton.setTextColorRes(R.color.icon_inactive)
            actionButton.setCompoundDrawableTint(actionButton.context.getColor(R.color.icon_inactive))
        }
    }
}
