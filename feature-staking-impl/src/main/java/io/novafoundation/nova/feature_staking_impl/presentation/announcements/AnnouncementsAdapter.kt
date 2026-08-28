package io.novafoundation.nova.feature_staking_impl.presentation.announcements

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_staking_impl.R

class AnnouncementsAdapter : BaseListAdapter<AnnouncementModel, AnnouncementHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementHolder {
        return AnnouncementHolder(parent.inflateChild(R.layout.item_announcement) as AlertView)
    }

    override fun onBindViewHolder(holder: AnnouncementHolder, position: Int) {
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

class AnnouncementHolder(private val alertView: AlertView) : BaseViewHolder(alertView) {

    fun bind(model: AnnouncementModel) = with(alertView) {
        setStylePreset(model.stylePreset)
        setMessage(model.description)
    }
}
