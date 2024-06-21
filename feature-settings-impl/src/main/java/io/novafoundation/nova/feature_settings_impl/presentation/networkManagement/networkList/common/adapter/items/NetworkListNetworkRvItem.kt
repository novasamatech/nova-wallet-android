package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items

import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getMaskedRipple
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.custom.ConnectionStateModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkManagementListAdapter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkImage
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkLabel
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkTitle
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkStatus
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkStatusShimmer
import kotlinx.android.synthetic.main.item_network_settings.view.itemNetworkSubtitle

data class NetworkListNetworkRvItem(
    val chainIcon: Icon,
    val chainId: ChainId,
    val title: String,
    val subtitle: String?,
    val chainLabel: String?,
    val disabled: Boolean,
    val status: ConnectionStateModel?
) : NetworkListRvItem {

    override val id: String = chainId
}

class NetworkListNetworkViewHolder(
    parent: ViewGroup,
    private val imageLoader: ImageLoader,
    private val itemHandler: NetworkManagementListAdapter.ItemHandler
) : NetworkListViewHolder(parent.inflateChild(R.layout.item_network_settings)) {

    init {
        itemView.background = itemView.context.getMaskedRipple(cornerSizeInDp = 0)
    }

    override fun bind(item: NetworkListRvItem) = with(itemView) {
        require(item is NetworkListNetworkRvItem)

        itemView.setOnClickListener { itemHandler.onNetworkClicked(item.chainId) }

        itemNetworkImage.setIcon(item.chainIcon, imageLoader)
        itemNetworkTitle.text = item.title
        itemNetworkSubtitle.text = item.subtitle
        itemNetworkLabel.setTextOrHide(item.chainLabel)

        itemNetworkStatusShimmer.isVisible = item.status != null
        if (item.status != null) {
            itemNetworkStatus.setText(item.status.name)
            item.status.chainStatusColor?.let { itemNetworkStatus.setTextColor(it) }
            itemNetworkStatus.setDrawableStart(item.status.chainStatusIcon, paddingInDp = 6)
            itemNetworkStatus.setCompoundDrawableTint(item.status.chainStatusIconColor)
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
