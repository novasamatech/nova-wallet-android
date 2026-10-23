package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import android.content.Context
import coil.request.ImageRequest
import io.novafoundation.nova.common.resources.ResourceManager
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet

/** The subnet name for any sentence or title; an unnamed subnet reads "Subnet <netuid>" (as on iOS), never blank. */
fun BittensorSubnet.title(resources: ResourceManager): String = title { resources.getString(R.string.bittensor_subnet_number, it) }

fun BittensorSubnet.title(context: Context): String = title { context.getString(R.string.bittensor_subnet_number, it) }

internal inline fun BittensorSubnet.title(subnetNumber: (Int) -> String): String = name.ifBlank { subnetNumber(id.value) }

/** Title followed by the alpha symbol, e.g. "Chutes ش" or "Subnet 42 ش". */
fun BittensorSubnet.displayName(resources: ResourceManager): String = displayName(title(resources))

fun BittensorSubnet.displayName(context: Context): String = displayName(title(context))

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
