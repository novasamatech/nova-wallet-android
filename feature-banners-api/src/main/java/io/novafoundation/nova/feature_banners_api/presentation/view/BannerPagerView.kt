package io.novafoundation.nova.feature_banners_api.presentation.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.indexOfOrNull
import io.novafoundation.nova.feature_banners_api.R
import io.novafoundation.nova.feature_banners_api.presentation.BannerPageModel
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.ImageSwitchingController
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.ContentSwitchingController
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.InOutAnimators
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.AlphaInterpolatedAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.CompoundInterpolatedAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.FractionAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.InterpolationRange
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.OffsetXInterpolatedAnimator
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.getContentSwitchingController
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.getImageSwitchingController
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerBackground
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerContent
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
    private var pages: List<BannerPageModel> = listOf()

    private val contentController = getContentSwitchingController()

    private val backgroundSwitchingController = getImageSwitchingController()

    private var autoSwipeCallbackAdded = false
    private val autoSwipeDelay = 3.seconds.inWholeMilliseconds
    private val autoSwipeCallback = object : Runnable {
        override fun run() {
            scrollController.swipeToPage(PageOffset.NEXT)
            handler?.postDelayed(this, autoSwipeDelay)
        }
    }

    private var callback: Callback? = null

    init {
        View.inflate(context, R.layout.view_pager_banner, this)

        contentController.attachToParent(pagerBannerContent)
        backgroundSwitchingController.attachToParent(pagerBannerBackground)
    }

    fun setBanners(banners: List<BannerPageModel>) {
        this.pages = banners
        pagerBannerIndicators.setPagesSize(pages.size)

        contentController.setPayloads(pages.map { ContentSwitchingController.Payload(it.title, it.subtitle, it.image) })
        backgroundSwitchingController.setPayloads(pages.map { it.background })

        if (pages.isNotEmpty()) {
            selectPageImmediately(pages.first())
            rerunAutoSwipe()
        }
    }

    fun setClosable(closable: Boolean) {

    }

    fun closeCurrentPage() {

    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    override fun onScrollDirectionChanged(toPage: PageOffset) {}

    override fun onScrollToPage(pageOffset: Float, toPage: PageOffset) {
        val nextPage = (toPage.pageOffset + currentPage).wrapPage()

        pagerBannerIndicators.setAnimationProgress(pageOffset.absoluteValue, currentPage, nextPage)

        contentController.setAnimationState(pageOffset, currentPage, nextPage)
        backgroundSwitchingController.setAnimationState(pageOffset, currentPage, nextPage)
    }

    override fun onScrollFinished(pageOffset: PageOffset) {
        this.currentPage = (this.currentPage + pageOffset.pageOffset).wrapPage()
    }

    override fun invalidateScroll() {
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            startAutoSwipe()
        } else {
            stopAutoSwipe()
        }

        val isScrollIntercepted = scrollController.onTouchEvent(event)
        parent.requestDisallowInterceptTouchEvent(isScrollIntercepted)
        return isScrollIntercepted
    }

    override fun computeScroll() {
        scrollController.computeScroll()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        scrollController.setContainerWidth(width)
    }

    private fun selectPageImmediately(page: BannerPageModel) {
        val index = pages.indexOfOrNull(page) ?: return

        contentController.showPageImmediately(index)
        backgroundSwitchingController.showPageImmediately(index)

        pagerBannerIndicators.setCurrentPage(index)
    }

    private fun rerunAutoSwipe() {
        stopAutoSwipe()
        startAutoSwipe()
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


    interface Callback {
        fun onBannerClosed(page: BannerPageModel)
    }
}
