package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorValidatorBinding
import java.math.RoundingMode

class BittensorValidatorAdapter(
    private val onSelected: (BittensorValidatorUi) -> Unit,
    private val onInfo: (BittensorValidatorUi) -> Unit
) : ListAdapter<BittensorValidatorUi, BittensorValidatorAdapter.Holder>(DiffCallback) {

    private var selectedAddress: String? = null

    fun select(address: String?) {
        val previous = selectedAddress
        selectedAddress = address
        currentList.forEachIndexed { index, row ->
            if (row.validator.address == previous || row.validator.address == address) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemBittensorValidatorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        holder.bind(row, row.validator.address == selectedAddress, onSelected, onInfo)
    }

    class Holder(private val binder: ItemBittensorValidatorBinding) : RecyclerView.ViewHolder(binder.root) {

        fun bind(
            model: BittensorValidatorUi,
            selected: Boolean,
            onSelected: (BittensorValidatorUi) -> Unit,
            onInfo: (BittensorValidatorUi) -> Unit
        ) = with(binder) {
            val row = model.validator
            root.alpha = if (row.selectable) 1f else 0.5f
            choice.isChecked = selected
            choice.isEnabled = row.selectable
            ViewCompat.setAccessibilityDelegate(
                root,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = android.widget.RadioButton::class.java.name
                        info.isCheckable = true
                        info.isChecked = selected
                    }
                }
            )
            name.text = row.validator.name.ifBlank { row.address }
            val take = row.take.movePointRight(2).setScale(2, RoundingMode.HALF_EVEN).toStripTrailingZerosString()
            val fee = root.context.getString(R.string.bittensor_validator_take, take)
            subtitle.text = if (row.selectable) fee else fee + " · " + root.context.getString(R.string.staking_nominator_status_inactive)
            icon.setImageDrawable(model.icon)
            info.contentDescription = root.context.getString(R.string.bittensor_validator_address_info, name.text)
            info.setOnClickListener { onInfo(model) }
            root.setOnClickListener { if (row.selectable) onSelected(model) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BittensorValidatorUi>() {
        override fun areItemsTheSame(old: BittensorValidatorUi, new: BittensorValidatorUi) = old.validator.address == new.validator.address
        override fun areContentsTheSame(old: BittensorValidatorUi, new: BittensorValidatorUi) = old == new
    }
}
