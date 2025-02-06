package io.novafoundation.nova.common.view.banner.switcher

import io.novafoundation.nova.common.view.ClipableImageView
import io.novafoundation.nova.common.view.banner.ClipableImage
import io.novafoundation.nova.common.view.banner.switcher.animation.FractionAnimator

class ImageSwitchingController(
    inAnimator: FractionAnimator,
    outAnimator: FractionAnimator,
    imageViewFactory: () -> ClipableImageView
) : SwitchingController<ClipableImage, ClipableImageView>(inAnimator, outAnimator) {

    private val controllerViews by lazy { Pair(imageViewFactory(), imageViewFactory()) }

    override fun getViews(): Pair<ClipableImageView, ClipableImageView> {
        return controllerViews
    }

    override fun setViewPayload(view: ClipableImageView, payload: ClipableImage) {
        if (view.drawable != payload.drawable) {
            view.setImageDrawable(payload.drawable)
        }

        view.setClipPadding(payload.clipPadding)
    }
}
