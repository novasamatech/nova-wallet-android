package io.novafoundation.nova.feature_account_api.presenatation.chain

import android.widget.ImageView
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class ChainUi(
    val id: String,
    val name: String,
    val icon: String?
)

private val ASSET_ICON_PLACEHOLDER = R.drawable.ic_nova

fun ImageView.loadChainIcon(icon: String?, imageLoader: ImageLoader) {
    load(icon, imageLoader) {
        placeholder(R.drawable.bg_chain_placeholder)
        error(R.drawable.bg_chain_placeholder)
    }
}

fun ImageView.loadTokenIcon(icon: String?, imageLoader: ImageLoader) {
    load(icon, imageLoader) {
        fallback(ASSET_ICON_PLACEHOLDER)
    }
}

fun Chain.Asset.icon(): Icon {
    return iconUrl?.asIcon() ?: ASSET_ICON_PLACEHOLDER.asIcon()
}
