package io.novafoundation.nova.feature_banners_api.presentation.view.switcher

import android.graphics.Rect
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage
import io.novafoundation.nova.feature_banners_api.presentation.view.PageView

class ContentSwitchingController(
    private val clipMargin: Rect,
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

            view.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

            val imageModel = payload.clipableImage
            if (view.image.drawable != imageModel.drawable) {
                view.image.setImageDrawable(imageModel.drawable)
            }

            if (imageModel.clip) {
                view.setClipMargin(clipMargin)
            } else {
                view.setClipMargin(Rect())
            }

            view
        }
    }
}
