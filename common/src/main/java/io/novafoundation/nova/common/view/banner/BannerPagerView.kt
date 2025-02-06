package io.novafoundation.nova.common.view.banner

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.view.ClipableImageView
import io.novafoundation.nova.common.view.banner.switcher.ImageSwitchingController
import io.novafoundation.nova.common.view.banner.switcher.TitleSubtitleSwitchingController
import io.novafoundation.nova.common.view.banner.switcher.animation.AlphaInterpolatedAnimator
import io.novafoundation.nova.common.view.banner.switcher.animation.CompoundInterpolatedAnimator
import io.novafoundation.nova.common.view.banner.switcher.animation.FractionAnimator
import io.novafoundation.nova.common.view.banner.switcher.animation.InterpolationRange
import io.novafoundation.nova.common.view.banner.switcher.animation.OffsetXInterpolatedAnimator
import io.novafoundation.nova.common.view.banner.switcher.setSwitchingController
import io.novafoundation.nova.common.view.banner.switcher.titleWithSubtitle
import kotlinx.android.synthetic.main.view_pager_banner.view.bannerPagerContent
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerBackground
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerImage
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerIndicators
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

enum class AnimationDirection(val multiplier: Float) {
    LEFT(-1f), RIGHT(1f), NONE(0f)
}

class BannerPagerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), BannerPagerScrollController.ScrollCallback {

    private val scrollController = BannerPagerScrollController(context, this)

    private var currentPage = 0
    private var pages = listOf(
        BannerPageModel(
            "1",
            "Title 1 Long Text. No Nice Factory for text view",
            "Change your wallet to nova in one click",
            ClipableImage(context.getDrawable(R.drawable.ic_banner_warning)!!, clipPadding(8.dp)),
            ClipableImage(context.getDrawable(R.drawable.bg_networks_banner)!!, clipPadding(0)),
        ),
        BannerPageModel(
            "2", "Title 2 Something nice", "Say WOW to Polkadot",
            ClipableImage(context.getDrawable(R.drawable.ic_banner_delegate_image)!!, clipPadding(0)),
            ClipableImage(context.getDrawable(R.drawable.ic_tinder_gov_entry_banner_background)!!, clipPadding(0)),
        ),
        BannerPageModel(
            "3", "Title 3 Test again", "Change your wallet to nova in one click",
            ClipableImage(context.getDrawable(R.drawable.ic_networks_banner_image)!!, clipPadding(8)),
            ClipableImage(context.getDrawable(R.drawable.bg_crowdloan_banner)!!, clipPadding(0)),
        ),
        BannerPageModel(
            "4", "Title 4 Ads for polkadot ecosystem", "LoooooL",
            ClipableImage(context.getDrawable(R.drawable.ic_networks_banner_image)!!, clipPadding(8.dp)),
            ClipableImage(context.getDrawable(R.drawable.ic_banner_turquoise_gradient)!!, clipPadding(0)),
        )
    )

    private fun clipPadding(size: Int) = Rect(0, size, 0, size)

    private val textController = getTextSwitchingController()
    private val imageSwitchingController = getImageSwitchingController {
        ClipableImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    private val backgroundSwitchingController = getImageSwitchingController {
        ClipableImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    private var autoSwipeCallbackAdded = false
    private val autoSwipeDelay = 3.seconds.inWholeMilliseconds
    private val autoSwipeCallback = object : Runnable {
        override fun run() {
            scrollController.swipeToPage(PageOffset.NEXT)
            handler?.postDelayed(this, autoSwipeDelay)
        }
    }

    init {
        View.inflate(context, R.layout.view_pager_banner, this)

        bannerPagerContent.setSwitchingController(textController)
        pagerBannerImage.setSwitchingController(imageSwitchingController)
        pagerBannerBackground.setSwitchingController(backgroundSwitchingController)

        pagerBannerIndicators.setPagesSize(pages.size)

        startAutoSwipe()
    }

    fun closeCurrentPage() {
        
    }

    override fun onScrollDirectionChanged(toPage: PageOffset) {
        val nextPage = (toPage.pageOffset + currentPage).wrapPage()

        val direction = toPage.scrollDirection.animationDirection()
        textController.setInAnimator(getInAnimator(direction))
        textController.setOutAnimator(getOutAnimator(direction))

        imageSwitchingController.setInAnimator(getInAnimator(direction))
        imageSwitchingController.setOutAnimator(getOutAnimator(direction))

        backgroundSwitchingController.setInAnimator(AlphaInterpolatedAnimator(DecelerateInterpolator(), InterpolationRange(from = 0f, to = 1f)))
        backgroundSwitchingController.setOutAnimator(AlphaInterpolatedAnimator(DecelerateInterpolator(), InterpolationRange(from = 1f, to = 0f)))

        textController.setPayload(pages[currentPage].titleWithSubtitle(), pages[nextPage].titleWithSubtitle())
        imageSwitchingController.setPayload(pages[currentPage].image, pages[nextPage].image)
        backgroundSwitchingController.setPayload(pages[currentPage].background, pages[nextPage].background)
    }

    override fun onScrollToPage(pageOffset: Float, toPage: PageOffset) {
        val nextPage = (toPage.pageOffset + currentPage).wrapPage()

        pagerBannerIndicators.setAnimationProgress(pageOffset.absoluteValue, currentPage, nextPage)

        textController.setAnimationState(pageOffset)
        imageSwitchingController.setAnimationState(pageOffset)
        backgroundSwitchingController.setAnimationState(pageOffset)
    }

    override fun onScrollFinished(pageOffset: PageOffset) {
        this.currentPage = (this.currentPage + pageOffset.pageOffset).wrapPage()
    }

    override fun invalidateScroll() {
        invalidate()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return scrollController.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            startAutoSwipe()
        } else {
            stopAutoSwipe()
        }

        return scrollController.onTouchEvent(event)
    }

    override fun computeScroll() {
        scrollController.computeScroll()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        scrollController.setContainerWidth(width)
    }

    private fun stopAutoSwipe() {
        if (autoSwipeCallbackAdded) {
            handler?.removeCallbacks(autoSwipeCallback)

            autoSwipeCallbackAdded = false
        }
    }

    private fun startAutoSwipe() {
        if (autoSwipeCallbackAdded) return

        handler?.postDelayed(autoSwipeCallback, autoSwipeDelay)
        autoSwipeCallbackAdded = true
    }

    private fun Int.animationDirection(): AnimationDirection {
        return when {
            this < 0 -> AnimationDirection.LEFT
            this > 0 -> AnimationDirection.RIGHT
            else -> AnimationDirection.NONE
        }
    }

    private fun Int.wrapPage(): Int {
        val min = 0
        val max = pages.size - 1
        if (max == 0) return 0
        if (max < 0) return this

        return when {
            this > max -> 0
            this < min -> max
            else -> this
        }
    }

    private fun getImageSwitchingController(imageViewFactory: () -> ClipableImageView): ImageSwitchingController {
        return ImageSwitchingController(
            inAnimator = getInAnimator(AnimationDirection.LEFT), outAnimator = getOutAnimator(AnimationDirection.LEFT), imageViewFactory = imageViewFactory
        )
    }

    private fun getTextSwitchingController(): TitleSubtitleSwitchingController {
        return TitleSubtitleSwitchingController(
            inAnimator = getInAnimator(AnimationDirection.LEFT),
            outAnimator = getOutAnimator(AnimationDirection.LEFT),
            viewFactory = { BannerTitleSubtitleView(context) }
        )
    }

    private fun getInAnimator(direction: AnimationDirection) = getTextAnimator(
        offsetRange = InterpolationRange(from = 30.dpF * direction.multiplier, to = 0f), alphaRange = InterpolationRange(from = 0f, to = 1f)
    )

    private fun getOutAnimator(direction: AnimationDirection) = getTextAnimator(
        offsetRange = InterpolationRange(from = 0f, 30.dpF * -direction.multiplier), alphaRange = InterpolationRange(from = 1f, to = 0f)
    )

    private fun getTextAnimator(offsetRange: InterpolationRange, alphaRange: InterpolationRange): FractionAnimator {
        return CompoundInterpolatedAnimator(
            OffsetXInterpolatedAnimator(DecelerateInterpolator(), offsetRange), AlphaInterpolatedAnimator(DecelerateInterpolator(), alphaRange)
        )
    }
}
