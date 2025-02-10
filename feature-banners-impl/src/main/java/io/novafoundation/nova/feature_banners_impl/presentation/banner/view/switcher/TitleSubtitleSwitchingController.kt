package io.novafoundation.nova.feature_banners_impl.presentation.banner.view.switcher

import io.novafoundation.nova.feature_banners_api.presentation.BannerPageModel
import io.novafoundation.nova.feature_banners_impl.presentation.banner.view.BannerTitleSubtitleView
import io.novafoundation.nova.feature_banners_impl.presentation.banner.view.switcher.animation.FractionAnimator

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
