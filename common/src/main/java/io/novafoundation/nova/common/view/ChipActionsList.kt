package io.novafoundation.nova.common.view

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild

class ChipActionsModel(
    val action: String
)

class ChipActionsAdapter(
    private val handler: Handler
) : BaseListAdapter<ChipActionsModel, ChipActionsViewHolder>(DiffCallback()) {

    fun interface Handler {

        fun chipActionClicked(index: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipActionsViewHolder {
        return ChipActionsViewHolder(parent, handler)
    }

    override fun onBindViewHolder(holder: ChipActionsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ChipActionsViewHolder constructor(
    parent: ViewGroup,
    handler: ChipActionsAdapter.Handler
) : BaseViewHolder(parent.inflateChild(R.layout.item_chip_action)) {

    init {
        containerView.setOnClickListener { handler.chipActionClicked(bindingAdapterPosition) }
    }

    fun bind(item: ChipActionsModel) {
        containerView.itemChipAction.text = item.action
    }

    override fun unbind() {}
}

private class DiffCallback : DiffUtil.ItemCallback<ChipActionsModel>() {

    override fun areItemsTheSame(oldItem: ChipActionsModel, newItem: ChipActionsModel): Boolean {
        return oldItem.action == newItem.action
    }

    override fun areContentsTheSame(oldItem: ChipActionsModel, newItem: ChipActionsModel): Boolean {
        return true
    }
}
