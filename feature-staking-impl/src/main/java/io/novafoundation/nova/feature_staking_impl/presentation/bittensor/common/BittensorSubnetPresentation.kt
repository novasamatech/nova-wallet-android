package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet

fun BittensorSubnet.displayName(fallback: String): String {
    return listOf(name.ifBlank { fallback }, symbol).filter(String::isNotBlank).joinToString(" ")
}

fun BittensorSubnet.icon(): Icon = logoUrl?.asUrlIcon() ?: Icon.FromDrawableRes(R.drawable.ic_fallback_network_icon)

fun ImageRequest.Builder.bittensorSubnetIcon() {
    transformations(CircleCropTransformation())
    placeholder(R.drawable.ic_fallback_network_icon)
    fallback(R.drawable.ic_fallback_network_icon)
    error(R.drawable.ic_fallback_network_icon)
}
