package io.novafoundation.nova.feature_versions_impl.presentation.update.adapters

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.databinding.ItemUpdateNotificationBinding
import io.novafoundation.nova.feature_versions_impl.presentation.update.models.UpdateNotificationModel

class UpdateNotificationsAdapter : ListAdapter<UpdateNotificationModel, UpdateNotificationHolder>(
    UpdateNotificationsDiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateNotificationHolder {
        return UpdateNotificationHolder(ItemUpdateNotificationBinding.inflate(parent.inflater(), parent, false))
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

class UpdateNotificationHolder(private val binder: ItemUpdateNotificationBinding) : GroupedListHolder(binder.root) {

    init {
        binder.itemNotificationLatest.background = binder.root.context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 6)
    }

    fun bind(item: UpdateNotificationModel) {
        binder.itemNotificationVersion.text = itemView.context.getString(R.string.update_notification_item_version, item.version)
        binder.itemNotificationDescription.text = item.changelog
        binder.itemNotificationDescription.isVisible = item.changelog != null

        binder.itemNotificationSeverity.isGone = item.severity == null
        binder.itemNotificationSeverity.text = item.severity
        item.severityColorRes?.let { binder.itemNotificationSeverity.setTextColorRes(it) }
        item.severityBackgroundRes?.let { binder.itemNotificationSeverity.background = itemView.context.getRoundedCornerDrawable(it, cornerSizeInDp = 6) }

        binder.itemNotificationLatest.isVisible = item.isLatestUpdate
        binder.itemNotificationDate.text = item.date
    }
}
