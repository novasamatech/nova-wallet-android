package io.novafoundation.nova.common.list

import android.view.View.OnClickListener
import android.view.ViewGroup
import io.novafoundation.nova.common.databinding.ItemPlaceholderBinding
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.PlaceholderModel

class EditablePlaceholderAdapter(
    private var model: PlaceholderModel? = null,
    private var padding: ViewSpace? = null,
    private var clickListener: OnClickListener? = null
) : SingleItemAdapter<EditableStubHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditableStubHolder {
        return EditableStubHolder(ItemPlaceholderBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: EditableStubHolder, position: Int) {
        holder.bind(model, padding, clickListener)
    }

    fun setPadding(padding: ViewSpace?) {
        this.padding = padding
        if (showItem) {
            notifyItemChanged(0)
        }
    }

    fun setPlaceholderData(model: PlaceholderModel) {
        this.model = model
        if (showItem) {
            notifyItemChanged(0)
        }
    }

    fun setButtonClickListener(listener: OnClickListener?) {
        clickListener = listener
        if (showItem) {
            notifyItemChanged(0)
        }
    }
}

class EditableStubHolder(private val binder: ItemPlaceholderBinding) : BaseViewHolder(binder.root) {

    fun bind(model: PlaceholderModel?, padding: ViewSpace?, clickListener: OnClickListener?) {
        model?.let { binder.itemPlaceholder.setModel(model) }
        binder.itemPlaceholder.setButtonClickListener(clickListener)
        padding?.let { binder.root.updatePadding(it) }
    }
}
