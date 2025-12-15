package io.novafoundation.nova.common.list.instruction

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.databinding.ItemInstructionBinding
import io.novafoundation.nova.common.databinding.ItemInstructionImageBinding
import io.novafoundation.nova.common.utils.inflater

private const val STEP_VIEW_TYPE = 0
private const val IMAGE_VIEW_TYPE = 1

class InstructionAdapter : ListAdapter<InstructionItem, ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when {
            viewType == STEP_VIEW_TYPE -> InstructionStepViewHolder(ItemInstructionBinding.inflate(parent.inflater(), parent, false))
            viewType == IMAGE_VIEW_TYPE -> InstructionImageViewHolder(ItemInstructionImageBinding.inflate(parent.inflater(), parent, false))
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is InstructionStepViewHolder -> holder.bind(getItem(position) as InstructionItem.Step)
            is InstructionImageViewHolder -> holder.bind(getItem(position) as InstructionItem.Image)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is InstructionItem.Image -> IMAGE_VIEW_TYPE
            is InstructionItem.Step -> STEP_VIEW_TYPE
        }
    }
}

private object DiffCallback : DiffUtil.ItemCallback<InstructionItem>() {
    override fun areItemsTheSame(oldItem: InstructionItem, newItem: InstructionItem): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: InstructionItem, newItem: InstructionItem): Boolean {
        return false
    }
}

class InstructionStepViewHolder(private val binder: ItemInstructionBinding) : ViewHolder(binder.root) {
    fun bind(instruction: InstructionItem.Step) {
        binder.instructionStep.setStepNumber(instruction.number)
        binder.instructionStep.setStepText(instruction.text)
    }
}

class InstructionImageViewHolder(private val binder: ItemInstructionImageBinding) : ViewHolder(binder.root) {
    fun bind(instruction: InstructionItem.Image) {
        binder.instructionImage.setModel(instruction.image, instruction.label)
    }
}
