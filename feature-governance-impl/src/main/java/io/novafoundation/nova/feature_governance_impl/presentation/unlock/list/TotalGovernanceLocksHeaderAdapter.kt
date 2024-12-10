package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ItemGovernanceTotalLocksHeaderBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class TotalGovernanceLocksHeaderAdapter : RecyclerView.Adapter<TotalGovernanceLocksHeaderAdapter.HeaderHolder>() {

    private var amount: AmountModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(ItemGovernanceTotalLocksHeaderBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(amount)
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setAmount(amountModel: AmountModel) {
        this.amount = amountModel
        notifyItemChanged(0, true)
    }

    inner class HeaderHolder(private val binder: ItemGovernanceTotalLocksHeaderBinding) : RecyclerView.ViewHolder(binder.root) {
        init {
            binder.root.background = binder.root.context.getRoundedCornerDrawable(R.color.block_background)
        }

        fun bind(amount: AmountModel?) {
            binder.governanceTotalLocksAmount.setAmount(amount)
        }
    }
}
