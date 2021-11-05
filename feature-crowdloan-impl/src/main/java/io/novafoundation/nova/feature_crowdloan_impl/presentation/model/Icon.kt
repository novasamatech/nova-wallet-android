package io.novafoundation.nova.feature_crowdloan_impl.presentation.model

import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressIcon
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan

sealed class Icon {

    class FromLink(val data: String) : Icon()

    class FromDrawable(val data: Drawable) : Icon()
}

fun ImageView.setIcon(icon: Icon, imageLoader: ImageLoader) {
    when (icon) {
        is Icon.FromDrawable -> setImageDrawable(icon.data)
        is Icon.FromLink -> load(icon.data, imageLoader)
    }
}

suspend fun generateCrowdloanIcon(
    crowdloan: Crowdloan,
    depositorAddress: String,
    iconGenerator: AddressIconGenerator,
): Icon {
    return if (crowdloan.parachainMetadata != null) {
        Icon.FromLink(crowdloan.parachainMetadata.iconLink)
    } else {
        val icon = iconGenerator.createAddressIcon(depositorAddress, AddressIconGenerator.SIZE_BIG)

        return Icon.FromDrawable(icon)
    }
}
