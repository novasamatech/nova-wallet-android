package io.novafoundation.nova.feature_versions_impl.presentation.update

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.databinding.ItemUpdateNotificationBinding
import io.novafoundation.nova.feature_versions_impl.databinding.ItemUpdateNotificationHeaderBinding
import io.novafoundation.nova.feature_versions_impl.databinding.ItemUpdateNotificationSeeAllBinding

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
            TYPE_HEADER -> UpdateNotificationBannerHolder(ItemUpdateNotificationHeaderBinding.inflate(parent.inflater(), parent, false))
            TYPE_VERSION -> UpdateNotificationHolder(ItemUpdateNotificationBinding.inflate(parent.inflater(), parent, false))
            TYPE_SEE_ALL -> SeeAllButtonHolder(ItemUpdateNotificationSeeAllBinding.inflate(parent.inflater(), parent, false), seeAllClickedListener)
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

class UpdateNotificationBannerHolder(private val binder: ItemUpdateNotificationHeaderBinding) : GroupedListHolder(binder.root) {

    fun bind(item: UpdateNotificationBannerModel) {
        binder.itemUpdateNotificationBanner.setImage(item.iconRes)
        binder.itemUpdateNotificationBanner.setBannerBackground(item.backgroundRes)
        binder.itemUpdateNotificationAlertTitle.text = item.title
        binder.itemUpdateNotificationAlertSubtitle.text = item.message
    }
}

class UpdateNotificationHolder(private val binder: ItemUpdateNotificationBinding) : GroupedListHolder(binder.root) {

    init {
        binder.itemNotificationLatest.background = binder.root.context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 6)
    }

    fun bind(item: UpdateNotificationModel) {
        binder.itemNotificationVersion.text = itemView.context.getString(R.string.update_notification_item_version, item.version)
        binder.itemNotificationDescription.text = item.changelog

        binder.itemNotificationSeverity.isGone = item.severity == null
        binder.itemNotificationSeverity.text = item.severity
        item.severityColorRes?.let { binder.itemNotificationSeverity.setTextColorRes(it) }
        item.severityBackgroundRes?.let { binder.itemNotificationSeverity.background = itemView.context.getRoundedCornerDrawable(it, cornerSizeInDp = 6) }

        binder.itemNotificationLatest.isVisible = item.isLatestUpdate
        binder.itemNotificationDate.text = item.date
    }
}

class SeeAllButtonHolder(
    private val binder: ItemUpdateNotificationSeeAllBinding,
    seeAllClickedListener: UpdateNotificationsAdapter.SeeAllClickedListener
) : GroupedListHolder(binder.root) {

    init {
        binder.root.setOnClickListener { seeAllClickedListener.onSeeAllClicked() }
    }
}
