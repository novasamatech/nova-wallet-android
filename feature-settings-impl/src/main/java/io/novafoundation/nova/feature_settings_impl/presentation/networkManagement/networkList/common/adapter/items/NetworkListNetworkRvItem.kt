package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items

import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkImage
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkLabel
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkTitle
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkStatus
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkStatusShimmer
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkSubtitle

class NetworkListNetworkRvItem(
    val chainIcon: Icon,
    val chainId: ChainId,
    val title: String,
    val subtitle: String?,
    val chainLabel: String?,
    val disabled: Boolean,
    val status: ConnectionState?
) : NetworkListRvItem {

    override val id: String = chainId

    class ConnectionState(
        val name: String,
        val chainStatusColor: Int,
        val chainStatusIcon: Int,
        val chainStatusIconColor: Int,
    )
}

class NetworkListNetworkViewHolder(
    parent: ViewGroup,
    private val imageLoader: ImageLoader
) : NetworkListViewHolder(parent.inflateChild(R.layout.item_network_settings)) {

    override fun bind(item: NetworkListRvItem) = with(itemView) {
        require(item is NetworkListNetworkRvItem)

        itemNetworkImage.setIcon(item.chainIcon, imageLoader)
        itemNetworkTitle.text = item.title
        itemNetworkSubtitle.text = item.subtitle
        itemNetworkStatusShimmer.isVisible = item.chainLabel != null
        itemNetworkLabel.setTextOrHide(item.chainLabel)

        itemNetworkStatusShimmer.isVisible = item.status != null
        if (item.status != null) {
            itemNetworkStatus.setTextOrHide(item.status.name)
            itemNetworkStatus.setTextColor(item.status.chainStatusColor)
            itemNetworkStatus.setDrawableStart(item.status.chainStatusIcon, paddingInDp = 6)
            itemNetworkStatus.setTextColor(item.status.chainStatusIconColor)
        }

        if (item.disabled) {
            itemNetworkImage.alpha = 0.32f
            itemNetworkTitle.setTextColorRes(R.color.text_secondary)
        } else {
            itemNetworkImage.alpha = 1f
            itemNetworkTitle.setTextColorRes(R.color.text_primary)
        }
    }
}
