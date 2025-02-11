package io.novafoundation.nova.feature_banners_api.presentation.view.switcher

import android.graphics.drawable.Drawable
import android.widget.ImageView
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage

class ImageSwitchingController(
    rightSwitchingAnimators: InOutAnimators,
    leftSwitchingAnimators: InOutAnimators,
    private val imageViewFactory: () -> ImageView
) : SwitchingController<Drawable, ImageView>(rightSwitchingAnimators = rightSwitchingAnimators, leftSwitchingAnimators = leftSwitchingAnimators) {

    override fun setPayloadsInternal(payloads: List<Drawable>): List<ImageView> {
        return payloads.map {
            val image = imageViewFactory()
            image.setImageDrawable(it)
            image
        }
    }
}
