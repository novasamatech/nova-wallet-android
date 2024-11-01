package io.novafoundation.nova.feature_account_api.presenatation.chain

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
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
        fallback(R.drawable.ic_fallback_network_icon)
    }
}

fun ImageLoader.loadChainIconToTarget(icon: String?, context: Context, target: (Drawable) -> Unit) {
    val request = ImageRequest.Builder(context)
        .data(icon)
        .placeholder(R.drawable.bg_chain_placeholder)
        .error(R.drawable.bg_chain_placeholder)
        .fallback(R.drawable.ic_fallback_network_icon)
        .target { target(it) }
        .build()

    this.enqueue(request)
}

fun ImageView.loadTokenIcon(icon: String?, imageLoader: ImageLoader) {
    load(icon, imageLoader) {
        fallback(ASSET_ICON_PLACEHOLDER)
    }
}

fun Chain.iconOrFallback(): Icon {
    return icon?.asIcon() ?: chainIconFallback()
}

fun String?.asIconOrFallback(): Icon {
    return this?.asIcon() ?: chainIconFallback()
}

fun chainIconFallback(): Icon {
    return R.drawable.ic_fallback_network_icon.asIcon()
}

fun AssetIconProvider.getAssetIconOrFallback(asset: Chain.Asset, fallbackIcon: Icon = AssetIconProvider.fallbackIcon): Icon {
    return this.getAssetIconOrFallback(asset.icon, fallbackIcon)
}

fun AssetIconProvider.getAssetIconOrFallback(asset: Chain.Asset, iconMode: AssetIconMode, fallbackIcon: Icon = AssetIconProvider.fallbackIcon): Icon {
    return this.getAssetIconOrFallback(asset.icon, iconMode, fallbackIcon)
}
