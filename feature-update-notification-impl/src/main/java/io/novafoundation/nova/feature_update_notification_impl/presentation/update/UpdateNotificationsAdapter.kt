package io.novafoundation.nova.feature_update_notification_impl.presentation.update

import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.item_update_notification.view.chipLabelView
import kotlinx.android.synthetic.main.item_update_notification.view.itemNotificationDate
import kotlinx.android.synthetic.main.item_update_notification.view.itemNotificationDescription
import kotlinx.android.synthetic.main.item_update_notification.view.itemNotificationVersion
import kotlinx.android.synthetic.main.item_update_notification_header.view.itemUpdateNotificationAlert

class UpdateNotificationsAdapter : GroupedListAdapter<UpdateNotificationAlertModel, UpdateNotificationModel>(
    DiffCallback
) {
    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_update_notification_header)

        return UpdateNotificationAlertHolder(view)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_update_notification)

        return UpdateNotificationHolder(view)
    }

    override fun bindGroup(holder: GroupedListHolder, group: UpdateNotificationAlertModel) {
        require(holder is UpdateNotificationAlertHolder)
        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: UpdateNotificationModel) {
        require(holder is UpdateNotificationHolder)
        holder.bind(child)
    }
}

private object DiffCallback : BaseGroupedDiffCallback<UpdateNotificationAlertModel, UpdateNotificationModel>(
    UpdateNotificationAlertModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: UpdateNotificationAlertModel, newItem: UpdateNotificationAlertModel): Boolean {
        return newItem.message == oldItem.message
    }

    override fun areChildItemsTheSame(oldItem: UpdateNotificationModel, newItem: UpdateNotificationModel): Boolean {
        return oldItem.versionTitle == newItem.versionTitle
    }

    override fun areGroupContentsTheSame(oldItem: UpdateNotificationAlertModel, newItem: UpdateNotificationAlertModel): Boolean {
        return true
    }

    override fun areChildContentsTheSame(oldItem: UpdateNotificationModel, newItem: UpdateNotificationModel): Boolean {
        return true
    }
}

class UpdateNotificationAlertHolder(view: View) : GroupedListHolder(view) {

    fun bind(item: UpdateNotificationAlertModel) {
        itemView.itemUpdateNotificationAlert.setText(item.message)
    }
}

class UpdateNotificationHolder(view: View) : GroupedListHolder(view) {

    fun bind(item: UpdateNotificationModel) {
        itemView.chipLabelView.text = item.updateType
        itemView.itemNotificationVersion.text = item.versionTitle
        itemView.itemNotificationDescription.text = item.versionDescription
        itemView.itemNotificationDate.text = item.date
    }
}
