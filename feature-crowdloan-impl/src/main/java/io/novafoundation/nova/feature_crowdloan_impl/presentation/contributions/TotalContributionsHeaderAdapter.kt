package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class TotalContributionsHeaderAdapter : RecyclerView.Adapter<TotalContributionsHeaderAdapter.HeaderHolder>() {

    private var amount: AmountModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(parent.inflateChild(R.layout.item_contributions_header))
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

    inner class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.background = view.context.getRoundedCornerDrawable(R.color.block_background)
        }

        fun bind(amount: AmountModel?) {
            itemView.totalContributedAmount.setAmount(amount)
        }
    }
}
