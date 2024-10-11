package io.novafoundation.nova.feature_versions_impl.presentation.update

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_versions_impl.R

class UpdateNotificationsAdapter(private val seeAllClickedListener: SeeAllClickedListener) : ListAdapter<Any, ViewHolder>(
    DiffCallback
) {

    interface SeeAllClickedListener {
        fun onSeeAllClicked()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VERSION = 1
        private const val TYPE_SEE_ALL = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> UpdateNotificationBannerHolder(parent.inflateChild(R.layout.item_update_notification_header))
            TYPE_VERSION -> UpdateNotificationHolder(parent.inflateChild(R.layout.item_update_notification))
            TYPE_SEE_ALL -> SeeAllButtonHolder(parent.inflateChild(R.layout.item_update_notification_see_all), seeAllClickedListener)
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UpdateNotificationBannerHolder -> holder.bind(item as UpdateNotificationBannerModel)
            is UpdateNotificationHolder -> holder.bind(item as UpdateNotificationModel)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is UpdateNotificationBannerModel -> TYPE_HEADER
            is UpdateNotificationModel -> TYPE_VERSION
            is SeeAllButtonModel -> TYPE_SEE_ALL
            else -> throw IllegalStateException()
        }
    }
}

private object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

class UpdateNotificationBannerHolder(view: View) : GroupedListHolder(view) {

    fun bind(item: UpdateNotificationBannerModel) {
        itemView.itemUpdateNotificationBanner.setImage(item.iconRes)
        itemView.itemUpdateNotificationBanner.setBannerBackground(item.backgroundRes)
        itemView.itemUpdateNotificationAlertTitle.text = item.title
        itemView.itemUpdateNotificationAlertSubtitle.text = item.message
    }
}

class UpdateNotificationHolder(view: View) : GroupedListHolder(view) {

    init {
        itemView.itemNotificationLatest.background = view.context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 6)
    }

    fun bind(item: UpdateNotificationModel) {
        itemView.itemNotificationVersion.text = itemView.context.getString(R.string.update_notification_item_version, item.version)
        itemView.itemNotificationDescription.text = item.changelog

        itemView.itemNotificationSeverity.isGone = item.severity == null
        itemView.itemNotificationSeverity.text = item.severity
        item.severityColorRes?.let { itemView.itemNotificationSeverity.setTextColorRes(it) }
        item.severityBackgroundRes?.let { itemView.itemNotificationSeverity.background = itemView.context.getRoundedCornerDrawable(it, cornerSizeInDp = 6) }

        itemView.itemNotificationLatest.isVisible = item.isLatestUpdate
        itemView.itemNotificationDate.text = item.date
    }
}

class SeeAllButtonHolder(view: View, seeAllClickedListener: UpdateNotificationsAdapter.SeeAllClickedListener) : GroupedListHolder(view) {

    init {
        view.setOnClickListener { seeAllClickedListener.onSeeAllClicked() }
    }
}
