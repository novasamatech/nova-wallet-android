package io.novafoundation.nova.feature_banners_impl.presentation.banner.view.switcher

import android.graphics.Rect
import io.novafoundation.nova.common.view.ClipableImageView
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage
import io.novafoundation.nova.feature_banners_impl.presentation.banner.view.switcher.animation.FractionAnimator

class ImageSwitchingController(
    private val clipPadding: Rect,
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

        if (payload.clip) {
            view.setClipPadding(clipPadding)
        } else {
            view.setClipPadding(Rect())
        }
    }
}
