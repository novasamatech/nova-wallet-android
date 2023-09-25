package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.item_start_staking_landing_title.view.itemStakingLandingTitle

class StartStakingLandingHeaderAdapter : SingleItemAdapter<StartStakingLandingHeaderViewHolder>() {

    private var title: CharSequence = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartStakingLandingHeaderViewHolder {
        return StartStakingLandingHeaderViewHolder(parent.inflateChild(R.layout.item_start_staking_landing_title))
    }

    override fun onBindViewHolder(holder: StartStakingLandingHeaderViewHolder, position: Int) {
        holder.bind(title)
    }

    fun setTitle(title: CharSequence) {
        this.title = title
        notifyItemChanged(0)
    }
}

class StartStakingLandingHeaderViewHolder(containerView: View) : ViewHolder(containerView) {

    fun bind(title: CharSequence) {
        with(itemView) {
            itemStakingLandingTitle.text = title
        }
    }
}
