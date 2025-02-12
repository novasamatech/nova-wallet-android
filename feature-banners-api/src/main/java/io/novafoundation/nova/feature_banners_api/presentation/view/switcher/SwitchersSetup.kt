package io.novafoundation.nova.feature_banners_api.presentation.view.switcher

import android.graphics.Rect
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
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

fun BannerPagerView.getImageSwitchingController(interpolator: Interpolator): ImageSwitchingController {
    return ImageSwitchingController(
        rightSwitchingAnimators = alphaAnimator(interpolator),
        leftSwitchingAnimators = alphaAnimator(interpolator),
        imageViewFactory = {
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
        }
    )
}

private fun alphaAnimator(interpolator: Interpolator): InOutAnimators {
    return InOutAnimators(
        inAnimator = AlphaInterpolatedAnimator(interpolator, InterpolationRange(0f, 1f)),
        outAnimator = AlphaInterpolatedAnimator(interpolator, InterpolationRange(1f, 0f))
    )
}

fun BannerPagerView.getContentSwitchingController(interpolator: Interpolator): ContentSwitchingController {
    return ContentSwitchingController(
        clipMargin = Rect(0, 8.dp, 0, 8.dp),
        rightSwitchingAnimators = getRightAnimator(interpolator),
        leftSwitchingAnimators = getLeftAnimator(interpolator),
        viewFactory = {
            PageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    )
}

private fun BannerPagerView.getRightAnimator(interpolator: Interpolator) = InOutAnimators(
    inAnimator = getContentAnimator(
        interpolator = interpolator,
        offsetRange = InterpolationRange(from = OFFSET.dpF, to = 0f),
        alphaRange = InterpolationRange(from = 0f, to = 1f)
    ),
    outAnimator = getContentAnimator(
        interpolator = interpolator,
        offsetRange = InterpolationRange(from = 0f, to = -OFFSET.dpF),
        alphaRange = InterpolationRange(from = 1f, to = 0f)
    )
)

private fun BannerPagerView.getLeftAnimator(interpolator: Interpolator) = InOutAnimators(
    inAnimator = getContentAnimator(
        interpolator = interpolator,
        offsetRange = InterpolationRange(from = -OFFSET.dpF, to = 0f),
        alphaRange = InterpolationRange(from = 0f, to = 1f)
    ),
    outAnimator = getContentAnimator(
        interpolator = interpolator,
        offsetRange = InterpolationRange(from = 0f, to = OFFSET.dpF),
        alphaRange = InterpolationRange(from = 1f, to = 0f)
    )
)

private fun getContentAnimator(interpolator: Interpolator, offsetRange: InterpolationRange, alphaRange: InterpolationRange): FractionAnimator {
    return CompoundInterpolatedAnimator(
        OffsetXInterpolatedAnimator(interpolator, offsetRange),
        AlphaInterpolatedAnimator(interpolator, alphaRange)
    )
}
