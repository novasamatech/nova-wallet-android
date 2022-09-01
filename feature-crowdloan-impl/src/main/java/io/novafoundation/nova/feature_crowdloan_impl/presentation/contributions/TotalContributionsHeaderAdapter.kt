package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.item_contributions_header.view.totalContributionsFiat
import kotlinx.android.synthetic.main.item_contributions_header.view.totalContributionsValue

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
            view.background = view.context.getRoundedCornerDrawable(R.color.white_8)
        }

        fun bind(amount: AmountModel?) {
            itemView.totalContributionsValue.text = amount?.token
            itemView.totalContributionsFiat.text = amount?.fiat
        }
    }
}
