package io.novafoundation.nova.feature_crowdloan_impl.presentation.model

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressIcon
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata

suspend fun generateCrowdloanIcon(
    parachainMetadata: ParachainMetadata?,
    depositorAddress: String,
    iconGenerator: AddressIconGenerator,
): Icon {
    return if (parachainMetadata != null) {
        Icon.FromLink(parachainMetadata.iconLink)
    } else {
        val icon = iconGenerator.createAddressIcon(depositorAddress, AddressIconGenerator.SIZE_BIG)

        Icon.FromDrawable(icon)
    }
}
