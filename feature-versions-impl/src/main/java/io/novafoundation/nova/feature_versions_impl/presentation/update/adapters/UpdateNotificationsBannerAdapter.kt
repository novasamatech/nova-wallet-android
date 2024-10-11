package io.novafoundation.nova.feature_versions_impl.presentation.update.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.presentation.update.models.UpdateNotificationBannerModel

class UpdateNotificationsBannerAdapter : RecyclerView.Adapter<UpdateNotificationBannerHolder>() {

    private var bannerModel: UpdateNotificationBannerModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateNotificationBannerHolder {
        return UpdateNotificationBannerHolder(parent.inflateChild(R.layout.item_update_notification_header))
    }

    override fun onBindViewHolder(holder: UpdateNotificationBannerHolder, position: Int) {
        holder.bind(bannerModel ?: return)
    }

    override fun getItemCount(): Int {
        return if (bannerModel != null) 1 else 0
    }

    fun setModel(model: UpdateNotificationBannerModel?) {
        val newNotNull = model != null
        val oldNotNull = bannerModel != null

        bannerModel = model

        if (newNotNull != oldNotNull) {
            if (newNotNull) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
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
