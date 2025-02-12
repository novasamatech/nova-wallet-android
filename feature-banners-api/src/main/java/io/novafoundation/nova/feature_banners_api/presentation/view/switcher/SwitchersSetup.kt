package io.novafoundation.nova.feature_banners_api.presentation.view.switcher

import android.graphics.Rect
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_banners_api.presentation.view.BannerPagerView
import io.novafoundation.nova.feature_banners_api.presentation.view.PageView
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.AlphaInterpolatedAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.CompoundInterpolatedAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.FractionAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.InterpolationRange
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.OffsetXInterpolatedAnimator


private const val OFFSET = 36

fun BannerPagerView.getImageSwitchingController(): ImageSwitchingController {
    return ImageSwitchingController(
        rightSwitchingAnimators = alphaAnimator(),
        leftSwitchingAnimators = alphaAnimator(),
        imageViewFactory = {
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
        }
    )
}

private fun alphaAnimator(): InOutAnimators {
    return InOutAnimators(
        inAnimator = AlphaInterpolatedAnimator(DecelerateInterpolator(), InterpolationRange(0f, 1f)),
        outAnimator = AlphaInterpolatedAnimator(DecelerateInterpolator(), InterpolationRange(1f, 0f))
    )
}

fun BannerPagerView.getContentSwitchingController(): ContentSwitchingController {
    return ContentSwitchingController(
        clipMargin = Rect(0, 8.dp, 0, 8.dp),
        rightSwitchingAnimators = getRightAnimator(),
        leftSwitchingAnimators = getLeftAnimator(),
        viewFactory = {
            PageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    )
}

private fun BannerPagerView.getRightAnimator() = InOutAnimators(
    inAnimator = getContentAnimator(
        offsetRange = InterpolationRange(from = OFFSET.dpF, to = 0f),
        alphaRange = InterpolationRange(from = 0f, to = 1f)
    ),
    outAnimator = getContentAnimator(
        offsetRange = InterpolationRange(from = 0f, to = -OFFSET.dpF),
        alphaRange = InterpolationRange(from = 1f, to = 0f)
    )
)

private fun BannerPagerView.getLeftAnimator() = InOutAnimators(
    inAnimator = getContentAnimator(
        offsetRange = InterpolationRange(from = -OFFSET.dpF, to = 0f),
        alphaRange = InterpolationRange(from = 0f, to = 1f)
    ),
    outAnimator = getContentAnimator(
        offsetRange = InterpolationRange(from = 0f, to = OFFSET.dpF),
        alphaRange = InterpolationRange(from = 1f, to = 0f)
    )
)

private fun getContentAnimator(offsetRange: InterpolationRange, alphaRange: InterpolationRange): FractionAnimator {
    return CompoundInterpolatedAnimator(
        OffsetXInterpolatedAnimator(DecelerateInterpolator(), offsetRange),
        AlphaInterpolatedAnimator(DecelerateInterpolator(), alphaRange)
    )
}
