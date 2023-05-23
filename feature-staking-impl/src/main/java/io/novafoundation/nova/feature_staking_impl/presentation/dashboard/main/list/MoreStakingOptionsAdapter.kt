package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.StakingDashboardMoreOptionsView

class MoreStakingOptionsAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<DashboardSectionHolder>() {

    interface Handler {

        fun onMoreOptionsClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardSectionHolder {
        val item = StakingDashboardMoreOptionsView(parent.context)

        return DashboardSectionHolder(item, handler)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: DashboardSectionHolder, position: Int) {}
}

class DashboardSectionHolder(
    containerView: StakingDashboardMoreOptionsView,
    handler: MoreStakingOptionsAdapter.Handler
) : RecyclerView.ViewHolder(containerView) {

    init {
        containerView.setOnClickListener { handler.onMoreOptionsClicked() }
    }
}
