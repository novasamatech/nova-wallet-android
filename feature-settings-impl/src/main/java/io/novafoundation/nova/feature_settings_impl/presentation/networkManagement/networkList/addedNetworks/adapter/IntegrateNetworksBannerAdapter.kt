package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_settings_impl.databinding.ItemIntegrateNetworksBannerBinding

class NetworksBannerAdapter(
    private val itemHandler: ItemHandler
) : SingleItemAdapter<NetworkBannerViewHolder>() {

    interface ItemHandler {

        fun closeBannerClicked()

        fun bannerWikiLinkClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkBannerViewHolder {
        return NetworkBannerViewHolder(ItemIntegrateNetworksBannerBinding.inflate(parent.inflater(), parent, false), itemHandler)
    }

    override fun onBindViewHolder(holder: NetworkBannerViewHolder, position: Int) {
        // Not need to bind anything
    }
}

class NetworkBannerViewHolder(
    private val binder: ItemIntegrateNetworksBannerBinding,
    private val itemHandler: NetworksBannerAdapter.ItemHandler
) : RecyclerView.ViewHolder(binder.root) {

    init {
        with(binder) {
            integrateNetworkBannerClose.setOnClickListener { itemHandler.closeBannerClicked() }
            integrateNetworkBannerLink.setOnClickListener { itemHandler.bannerWikiLinkClicked() }
        }
    }
}
