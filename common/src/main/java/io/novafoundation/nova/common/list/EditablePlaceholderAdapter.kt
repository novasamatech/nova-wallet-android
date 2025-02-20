package io.novafoundation.nova.common.list

import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.PlaceholderModel
import kotlinx.android.synthetic.main.item_placeholder.view.itemPlaceholder

class EditablePlaceholderAdapter : SingleItemAdapter<EditableStubHolder>() {

    private var model: PlaceholderModel? = null
    private var padding: ViewSpace? = null
    private var clickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditableStubHolder {
        return EditableStubHolder(parent.inflateChild(R.layout.item_placeholder))
    }

    override fun onBindViewHolder(holder: EditableStubHolder, position: Int) {
        holder.bind(model, padding, clickListener)
    }

    fun setPadding(padding: ViewSpace?) {
        this.padding = padding
        if (showPlaceholder) {
            notifyItemChanged(0)
        }
    }

    fun setPlaceholderData(model: PlaceholderModel) {
        this.model = model
        if (showPlaceholder) {
            notifyItemChanged(0)
        }
    }

    fun setButtonClickListener(listener: OnClickListener?) {
        clickListener = listener
        if (showPlaceholder) {
            notifyItemChanged(0)
        }
    }
}

class EditableStubHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(model: PlaceholderModel?, padding: ViewSpace?, clickListener: OnClickListener?) {
        model?.let { itemView.itemPlaceholder.setModel(model) }
        itemView.itemPlaceholder.setButtonClickListener(clickListener)
        padding?.let { itemView.updatePadding(it) }
    }
}
