package io.novafoundation.nova.common.view.input.selector

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ItemListSelectorBinding
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator

class DynamicSelectorBottomSheet(
    context: Context,
    private val payload: Payload,
    onClicked: ClickHandler<ListSelectorMixin.Item>,
    onCancel: (() -> Unit)? = null,
) : DynamicListBottomSheet<ListSelectorMixin.Item>(
    context = context,
    payload = payload,
    diffCallback = SelectorDiffCallback(),
    onClicked = onClicked,
    onCancel = onCancel
) {

    class Payload(
        val titleRes: Int,
        val subtitle: String?,
        data: List<ListSelectorMixin.Item>
    ) : DynamicListBottomSheet.Payload<ListSelectorMixin.Item>(data)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.titleRes)
        binder.dynamicListSheetSubtitle.setTextOrHide(payload.subtitle)
    }

    override fun holderCreator(): HolderCreator<ListSelectorMixin.Item> = { parentView ->
        SelectorViewHolder(ItemListSelectorBinding.inflate(parentView.inflater(), parentView, false))
    }
}

private class SelectorViewHolder(private val binder: ItemListSelectorBinding) : DynamicListSheetAdapter.Holder<ListSelectorMixin.Item>(binder.root) {

    override fun bind(
        item: ListSelectorMixin.Item,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<ListSelectorMixin.Item>
    ) {
        super.bind(item, isSelected, handler)

        with(containerView) {
            binder.itemSelectorIcon.setImageResource(item.iconRes)
            binder.itemSelectorIcon.setImageTintRes(item.iconTintRes)
            binder.itemSelectorTitle.setText(item.titleRes)
            binder.itemSelectorTitle.setTextColorRes(item.titleColorRes)
        }
    }
}

private class SelectorDiffCallback : DiffUtil.ItemCallback<ListSelectorMixin.Item>() {

    override fun areItemsTheSame(oldItem: ListSelectorMixin.Item, newItem: ListSelectorMixin.Item): Boolean {
        return oldItem.titleRes == newItem.titleRes
    }

    override fun areContentsTheSame(oldItem: ListSelectorMixin.Item, newItem: ListSelectorMixin.Item): Boolean {
        return true
    }
}
