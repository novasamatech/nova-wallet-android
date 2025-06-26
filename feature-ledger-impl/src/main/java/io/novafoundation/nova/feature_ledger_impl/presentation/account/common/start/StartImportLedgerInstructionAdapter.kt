package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_ledger_impl.databinding.ItemImportLedgerInstructionBinding

class LedgerGuideItem(
    val number: Int,
    val text: CharSequence
)

class StartImportLedgerInstructionAdapter : ListAdapter<LedgerGuideItem, LedgerInstructionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerInstructionViewHolder {
        return LedgerInstructionViewHolder(ItemImportLedgerInstructionBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: LedgerInstructionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DiffCallback : DiffUtil.ItemCallback<LedgerGuideItem>() {
    override fun areItemsTheSame(oldItem: LedgerGuideItem, newItem: LedgerGuideItem): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: LedgerGuideItem, newItem: LedgerGuideItem): Boolean {
        return false
    }
}

class LedgerInstructionViewHolder(private val binder: ItemImportLedgerInstructionBinding) : ViewHolder(binder.root) {
    fun bind(instruction: LedgerGuideItem) {
        binder.startImportLedgerInstruction.setStepNumber(instruction.number)
        binder.startImportLedgerInstruction.setStepText(instruction.text)
    }
}
