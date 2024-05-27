package io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders

import android.view.ViewGroup
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.common.view.ChipLabelView

class AccountChipHolder(override val containerView: ChipLabelView) : GroupedListHolder(containerView) {

    init {
        val context = containerView.context

        containerView.layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16.dp(context), 16.dp(context), 0, 8.dp(context))
        }
    }

    fun bind(item: ChipLabelModel) {
        containerView.setModel(item)
    }
}
