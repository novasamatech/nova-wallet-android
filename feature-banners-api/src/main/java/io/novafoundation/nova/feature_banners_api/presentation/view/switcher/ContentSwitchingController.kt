package io.novafoundation.nova.feature_banners_api.presentation.view.switcher

import android.graphics.Rect
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage
import io.novafoundation.nova.feature_banners_api.presentation.view.PageView

class ContentSwitchingController(
    private val clipPadding: Rect,
    rightSwitchingAnimators: InOutAnimators,
    leftSwitchingAnimators: InOutAnimators,
    private val viewFactory: () -> PageView
) : SwitchingController<ContentSwitchingController.Payload, PageView>(
    rightSwitchingAnimators = rightSwitchingAnimators,
    leftSwitchingAnimators = leftSwitchingAnimators
) {

    class Payload(val title: String, val subtitle: String, val clipableImage: ClipableImage)

    override fun setPayloadsInternal(payloads: List<Payload>): List<PageView> {
        return payloads.map { payload ->
            val view = viewFactory()
            view.title.text = payload.title
            view.subtitle.text = payload.subtitle

            val imageModel = payload.clipableImage
            if (view.image.drawable != imageModel.drawable) {
                view.image.setImageDrawable(imageModel.drawable)
            }

            if (imageModel.clip) {
                view.image.setClipPadding(clipPadding)
            } else {
                view.image.setClipPadding(Rect())
            }

            view
        }
    }
}
