package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.AnnouncementModel

class DashboardAnnouncementsAdapter : BaseListAdapter<AnnouncementModel, DashboardAnnouncementHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardAnnouncementHolder {
        return DashboardAnnouncementHolder(parent.inflateChild(R.layout.item_dashboard_announcement) as AlertView)
    }

    override fun onBindViewHolder(holder: DashboardAnnouncementHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DiffCallback : DiffUtil.ItemCallback<AnnouncementModel>() {

    override fun areItemsTheSame(oldItem: AnnouncementModel, newItem: AnnouncementModel): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: AnnouncementModel, newItem: AnnouncementModel): Boolean {
        return oldItem == newItem
    }
}

class DashboardAnnouncementHolder(private val alertView: AlertView) : BaseViewHolder(alertView) {

    fun bind(model: AnnouncementModel) = with(alertView) {
        setStylePreset(model.stylePreset)
        setMessage(model.description)
    }
}
