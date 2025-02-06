package io.novafoundation.nova.common.view.banner.switcher

import io.novafoundation.nova.common.view.banner.BannerPageModel
import io.novafoundation.nova.common.view.banner.BannerTitleSubtitleView
import io.novafoundation.nova.common.view.banner.switcher.animation.FractionAnimator

class TitleSubtitleSwitchingController(
    inAnimator: FractionAnimator,
    outAnimator: FractionAnimator,
    viewFactory: () -> BannerTitleSubtitleView
) : SwitchingController<TitleSubtitleSwitchingController.Payload, BannerTitleSubtitleView>(inAnimator, outAnimator) {

    class Payload(val title: String, val subtitle: String)

    private val controllerViews by lazy { Pair(viewFactory(), viewFactory()) }

    override fun getViews(): Pair<BannerTitleSubtitleView, BannerTitleSubtitleView> {
        return controllerViews
    }

    override fun setViewPayload(view: BannerTitleSubtitleView, payload: Payload) {
        if (view.title.text == payload.title && view.subtitle.text == payload.subtitle) return

        view.title.text = payload.title
        view.subtitle.text = payload.subtitle
    }
}

fun BannerPageModel.titleWithSubtitle() = TitleSubtitleSwitchingController.Payload(title, subtitle)
