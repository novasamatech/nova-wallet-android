package io.novafoundation.nova.common.view

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.databinding.ItemChipActionBinding
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater

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
        return ChipActionsViewHolder(ItemChipActionBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: ChipActionsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ChipActionsViewHolder(
    private val binder: ItemChipActionBinding,
    handler: ChipActionsAdapter.Handler
) : BaseViewHolder(binder.root) {

    init {
        binder.root.setOnClickListener { handler.chipActionClicked(bindingAdapterPosition) }
    }

    fun bind(item: ChipActionsModel) {
        binder.itemChipAction.text = item.action
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
