package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorValidatorBinding
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
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
            if (row.eligibility.address == previous || row.eligibility.address == address) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemBittensorValidatorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        val row = getItem(position)
        holder.bind(
            row,
            row.eligibility.address == selectedAddress,
            onSelected,
            onInfo
        )
    }

    class Holder(private val binder: ItemBittensorValidatorBinding) : RecyclerView.ViewHolder(binder.root) {

        fun bind(
            model: BittensorValidatorUi,
            selected: Boolean,
            onSelected: (BittensorValidatorUi) -> Unit,
            onInfo: (BittensorValidatorUi) -> Unit
        ) = with(binder) {
            val row = model.eligibility
            root.alpha = if (row.canSelect) 1f else 0.5f
            choice.isChecked = selected && row.canSelect
            choice.isEnabled = row.canSelect
            bindAccessibility(selected && row.canSelect)
            name.text = bittensorValidatorDisplayName(row)
            subtitle.text = model.subtitle ?: formatSubtitle(row)
            apy.text = if (row.inactive) root.context.getString(R.string.staking_nominator_status_inactive) else model.apy
            apy.isVisible = row.inactive || model.apy != null
            apy.setTextColorRes(if (row.inactive) R.color.text_secondary else model.apyColorRes)
            icon.setImageDrawable(model.icon)
            info.contentDescription = root.context.getString(R.string.bittensor_validator_address_info, name.text)
            info.setOnClickListener { onInfo(model) }
            // Inactive rows stay tappable so the screen can explain why they cannot be selected.
            root.setOnClickListener { if (row.canSelect || row.inactive) onSelected(model) }
        }

        private fun bindAccessibility(selected: Boolean) {
            ViewCompat.setAccessibilityDelegate(
                binder.root,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = android.widget.RadioButton::class.java.name
                        info.isCheckable = true
                        info.isChecked = selected
                    }
                }
            )
        }

        private fun formatSubtitle(validator: BittensorSubnetValidator): String {
            val context = binder.root.context
            val commissionPercentage = validator.commissionRate.movePointRight(2)
                .setScale(2, RoundingMode.HALF_EVEN).toStripTrailingZerosString()
            return context.getString(R.string.bittensor_validator_take, commissionPercentage)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BittensorValidatorUi>() {
        override fun areItemsTheSame(old: BittensorValidatorUi, new: BittensorValidatorUi) = old.eligibility.address == new.eligibility.address
        override fun areContentsTheSame(old: BittensorValidatorUi, new: BittensorValidatorUi) = old == new
    }
}
