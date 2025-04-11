package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_staking_impl.databinding.ItemStartStakingLandingTitleBinding

class StartStakingLandingHeaderAdapter : SingleItemAdapter<StartStakingLandingHeaderViewHolder>() {

    private var title: CharSequence = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartStakingLandingHeaderViewHolder {
        return StartStakingLandingHeaderViewHolder(ItemStartStakingLandingTitleBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: StartStakingLandingHeaderViewHolder, position: Int) {
        holder.bind(title)
    }

    fun setTitle(title: CharSequence) {
        this.title = title
        notifyItemChanged(0)
    }
}

class StartStakingLandingHeaderViewHolder(private val binder: ItemStartStakingLandingTitleBinding) : ViewHolder(binder.root) {

    fun bind(title: CharSequence) {
        with(binder) {
            itemStakingLandingTitle.text = title
        }
    }
}
