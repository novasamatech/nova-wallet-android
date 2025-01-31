package io.novafoundation.nova.feature_versions_impl.presentation.update.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_versions_impl.databinding.ItemUpdateNotificationHeaderBinding
import io.novafoundation.nova.feature_versions_impl.presentation.update.models.UpdateNotificationBannerModel

class UpdateNotificationsBannerAdapter : RecyclerView.Adapter<UpdateNotificationBannerHolder>() {

    private var bannerModel: UpdateNotificationBannerModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateNotificationBannerHolder {
        return UpdateNotificationBannerHolder(ItemUpdateNotificationHeaderBinding.inflate(parent.inflater(), parent, false))
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

class UpdateNotificationBannerHolder(private val binder: ItemUpdateNotificationHeaderBinding) : GroupedListHolder(binder.root) {

    fun bind(item: UpdateNotificationBannerModel) {
        binder.itemUpdateNotificationBanner.setImage(item.iconRes)
        binder.itemUpdateNotificationBanner.setBannerBackground(item.backgroundRes)
        binder.itemUpdateNotificationAlertTitle.text = item.title
        binder.itemUpdateNotificationAlertSubtitle.text = item.message
    }
}
