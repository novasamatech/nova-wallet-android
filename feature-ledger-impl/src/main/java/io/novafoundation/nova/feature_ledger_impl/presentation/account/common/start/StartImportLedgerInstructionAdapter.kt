package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_ledger_impl.R
import kotlinx.android.synthetic.main.item_import_ledger_instruction.view.startImportLedgerInstruction

class LedgerGuideItem(
    val number: Int,
    val text: CharSequence
)

class StartImportLedgerInstructionAdapter : ListAdapter<LedgerGuideItem, LedgerInstructionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerInstructionViewHolder {
        return LedgerInstructionViewHolder(parent.inflateChild(R.layout.item_import_ledger_instruction))
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

class LedgerInstructionViewHolder(itemView: View) : ViewHolder(itemView) {
    fun bind(instruction: LedgerGuideItem) {
        itemView.startImportLedgerInstruction.setStepNumber(instruction.number)
        itemView.startImportLedgerInstruction.setStepText(instruction.text)
    }
}
