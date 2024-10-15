package io.novafoundation.nova.common.view.input.chooser

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ItemListChooserBinding
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin.Model

class ListChooserBottomSheet<T>(
    context: Context,
    private val payload: Payload<Model<T>>,
    onClicked: ClickHandler<Model<T>>,
    onCancel: (() -> Unit)? = null,
) : DynamicListBottomSheet<Model<T>>(
    context = context,
    payload = payload,
    diffCallback = DiffCallback(),
    onClicked = onClicked,
    onCancel = onCancel
) {

    class Payload<out T>(
        data: List<T>,
        selected: T? = null,
        @StringRes val titleRes: Int,
    ) : DynamicListBottomSheet.Payload<T>(data, selected)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.titleRes)
    }

    override fun holderCreator(): HolderCreator<Model<T>> = { parentView ->
        ListChooserViewHolder(ItemListChooserBinding.inflate(parentView.inflater(), parentView, false))
    }
}

private class ListChooserViewHolder<T>(private val binder: ItemListChooserBinding) : DynamicListSheetAdapter.Holder<Model<T>>(binder.root) {

    override fun bind(
        item: Model<T>,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<Model<T>>
    ) {
        super.bind(item, isSelected, handler)

        with(containerView) {
            binder.itemListChooserLabel.text = item.display
            binder.itemListChooserCheck.isChecked = isSelected
        }
    }
}

private class DiffCallback<T> : DiffUtil.ItemCallback<Model<T>>() {
    override fun areItemsTheSame(oldItem: Model<T>, newItem: Model<T>): Boolean {
        return oldItem.display == newItem.display
    }

    override fun areContentsTheSame(oldItem: Model<T>, newItem: Model<T>): Boolean {
        return true
    }
}
