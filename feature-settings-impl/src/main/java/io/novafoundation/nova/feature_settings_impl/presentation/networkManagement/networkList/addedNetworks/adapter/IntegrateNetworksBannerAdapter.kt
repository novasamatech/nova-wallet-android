package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_settings_impl.R

class NetworksBannerAdapter(
    private val itemHandler: ItemHandler
) : SingleItemAdapter<NetworkBannerViewHolder>() {

    interface ItemHandler {

        fun closeBannerClicked()

        fun bannerWikiLinkClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkBannerViewHolder {
        return NetworkBannerViewHolder(parent.inflateChild(R.layout.item_integrate_networks_banner), itemHandler)
    }

    override fun onBindViewHolder(holder: NetworkBannerViewHolder, position: Int) {
        // Not need to bind anything
    }
}

class NetworkBannerViewHolder(
    view: View,
    private val itemHandler: NetworksBannerAdapter.ItemHandler
) : RecyclerView.ViewHolder(view) {

    init {
        with(itemView) {
            integrateNetworkBannerClose.setOnClickListener { itemHandler.closeBannerClicked() }
            integrateNetworkBannerLink.setOnClickListener { itemHandler.bannerWikiLinkClicked() }
        }
    }
}
