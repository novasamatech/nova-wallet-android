package io.novafoundation.nova.feature_versions_impl.presentation.update.adapters

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.presentation.update.models.UpdateNotificationModel

class UpdateNotificationsAdapter : ListAdapter<UpdateNotificationModel, UpdateNotificationHolder>(
    UpdateNotificationsDiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateNotificationHolder {
        return UpdateNotificationHolder(parent.inflateChild(R.layout.item_update_notification))
    }

    override fun onBindViewHolder(holder: UpdateNotificationHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object UpdateNotificationsDiffCallback : DiffUtil.ItemCallback<UpdateNotificationModel>() {
    override fun areItemsTheSame(oldItem: UpdateNotificationModel, newItem: UpdateNotificationModel): Boolean {
        return oldItem.version == newItem.version
    }

    override fun areContentsTheSame(oldItem: UpdateNotificationModel, newItem: UpdateNotificationModel): Boolean {
        return true
    }
}

class UpdateNotificationHolder(view: View) : GroupedListHolder(view) {

    init {
        itemView.itemNotificationLatest.background = view.context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 6)
    }

    fun bind(item: UpdateNotificationModel) {
        itemView.itemNotificationVersion.text = itemView.context.getString(R.string.update_notification_item_version, item.version)
        itemView.itemNotificationDescription.text = item.changelog
        itemView.itemNotificationDescription.isVisible = item.changelog != null

        itemView.itemNotificationSeverity.isGone = item.severity == null
        itemView.itemNotificationSeverity.text = item.severity
        item.severityColorRes?.let { itemView.itemNotificationSeverity.setTextColorRes(it) }
        item.severityBackgroundRes?.let { itemView.itemNotificationSeverity.background = itemView.context.getRoundedCornerDrawable(it, cornerSizeInDp = 6) }

        itemView.itemNotificationLatest.isVisible = item.isLatestUpdate
        itemView.itemNotificationDate.text = item.date
    }
}
