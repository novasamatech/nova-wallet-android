package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_staking_impl.databinding.ItemStartStakingLandingTitleBinding

class StartStakingLandingHeaderAdapter : SingleItemAdapter<StartStakingLandingHeaderViewHolder>() {

    private var title: CharSequence = ""

    private var subtitle: CharSequence? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartStakingLandingHeaderViewHolder {
        return StartStakingLandingHeaderViewHolder(ItemStartStakingLandingTitleBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: StartStakingLandingHeaderViewHolder, position: Int) {
        holder.bind(title, subtitle)
    }

    fun setTitle(title: CharSequence, subtitle: CharSequence? = null) {
        this.title = title
        this.subtitle = subtitle
        notifyItemChanged(0)
    }
}

class StartStakingLandingHeaderViewHolder(private val binder: ItemStartStakingLandingTitleBinding) : ViewHolder(binder.root) {

    fun bind(title: CharSequence, subtitle: CharSequence?) {
        with(binder) {
            itemStakingLandingTitle.text = title
            itemStakingLandingSubtitle.setTextOrHide(subtitle)
        }
    }
}
