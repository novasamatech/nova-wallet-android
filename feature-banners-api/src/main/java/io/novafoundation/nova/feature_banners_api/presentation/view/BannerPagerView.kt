package io.novafoundation.nova.feature_banners_api.presentation.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.ViewClickGestureDetector
import io.novafoundation.nova.common.utils.indexOfOrNull
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_banners_api.R
import io.novafoundation.nova.feature_banners_api.presentation.BannerPageModel
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.ContentSwitchingController
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.getContentSwitchingController
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.getImageSwitchingController
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerBackground
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerClose
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerContent
import kotlinx.android.synthetic.main.view_pager_banner.view.pagerBannerIndicators
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

/**
 * View for viewing banner pages, supporting infinite scrolling
 * BannerPagerScrollController tracks banner scrolling and triggers a callback, passing a value from -1 to 1 depending on the scroll direction
 * Animates the scroll to the selected page upon release
 */
class BannerPagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), BannerPagerScrollController.ScrollCallback {

    private val scrollInterpolator = DecelerateInterpolator()

    private val scrollController = BannerPagerScrollController(context, this)

    private val gestureDetector = ViewClickGestureDetector(this)

    private var currentPage = 0
    private var pages: MutableList<BannerPageModel> = mutableListOf()

    private val canScroll: Boolean
        get() = !closeAnimator.isRunning && pages.size > 1

    private val canRunScrollAnimation: Boolean
        get() = canScroll && scrollController.isIdle()

    private val contentController = getContentSwitchingController(scrollInterpolator)

    private val backgroundSwitchingController = getImageSwitchingController(scrollInterpolator)

    private var autoSwipeCallbackAdded = false
    private val autoSwipeDelay = 4.seconds.inWholeMilliseconds
    private val autoSwipeCallback = object : Runnable {
        override fun run() {
            if (canRunScrollAnimation) {
                scrollController.swipeToPage(PageOffset.NEXT)
                postDelayed(this, autoSwipeDelay)
            }
        }
    }

    private var callback: Callback? = null

    private val closeAnimator = ValueAnimator().apply {
        interpolator = scrollInterpolator
        duration = scrollController.minimumScrollDuration.toLong()
    }

    val isClosable: Boolean
        get() = pagerBannerClose.isVisible

    init {
        View.inflate(context, R.layout.view_pager_banner, this)

        contentController.attachToParent(pagerBannerContent)
        backgroundSwitchingController.attachToParent(pagerBannerBackground)

        pagerBannerClose.setOnClickListener { closeCurrentPage() }
    }

    fun setBanners(banners: List<BannerPageModel>) {
        val newIds = banners.mapToSet { it.id }
        val currentIds = pages.mapToSet { it.id }
        if (newIds == currentIds) return // Check that pages not changed

        this.pages.clear()
        this.pages.addAll(banners)
        pagerBannerIndicators.setPagesSize(pages.size)

        contentController.setPayloads(pages.map { ContentSwitchingController.Payload(it.title, it.subtitle, it.image) })
        backgroundSwitchingController.setPayloads(pages.map { it.background })

        if (pages.isNotEmpty()) {
            selectPageImmediately(pages.first())
            rerunAutoSwipe()
        }
    }

    fun setClosable(closable: Boolean) {
        pagerBannerClose.isVisible = closable
    }

    fun closeCurrentPage() {
        if (pages.size == 1) {
            callback?.onBannerClosed(pages.first())

            return // Don't run animation to let close banner from outside
        }

        if (isClosable && canRunScrollAnimation) {
            val isLastPageAfterClose = pages.size == 2 // 2 pages befo
            val nextPage = (currentPage + 1).wrapPage()

            scrollController.setTouchable(false)
            stopAutoSwipe()

            closeAnimator.removeAllListeners()
            closeAnimator.removeAllUpdateListeners()

            closeAnimator.setFloatValues(0f, 1f)
            closeAnimator.addUpdateListener {
                if (isLastPageAfterClose) {
                    pagerBannerIndicators.alpha = 1f - it.animatedFraction
                } else {
                    pagerBannerIndicators.setCloseProgress(it.animatedFraction, currentPage, nextPage)
                }

                contentController.setAnimationState(it.animatedFraction, currentPage, nextPage)
                backgroundSwitchingController.setAnimationState(it.animatedFraction, currentPage, nextPage)
            }
            closeAnimator.doOnEnd {
                closePage(currentPage)
                invalidateScrolling()
                startAutoSwipe()
            }
            closeAnimator.start()
        }
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    private fun closePage(index: Int) {
        contentController.removePageAt(index)
        backgroundSwitchingController.removePageAt(index)
        val closedPage = pages.removeAt(index)
        currentPage = index.wrapPage()
        pagerBannerIndicators.setPagesSize(pages.size)
        pagerBannerIndicators.setCurrentPage(currentPage)
        contentController.showPageImmediately(currentPage)
        backgroundSwitchingController.showPageImmediately(currentPage)

        callback?.onBannerClosed(closedPage)
    }

    private fun invalidateScrolling() {
        scrollController.setTouchable(pages.size > 1)
    }

    override fun onScrollToPage(pageOffset: Float, toPage: PageOffset) {
        if (!canScroll) return

        val nextPage = (currentPage + toPage.pageOffset).wrapPage()

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
        gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP) {
            startAutoSwipe()
        } else {
            stopAutoSwipe()
        }

        val isScrollIntercepted = scrollController.onTouchEvent(event)
        parent.requestDisallowInterceptTouchEvent(isScrollIntercepted)
        return isScrollIntercepted
    }

    override fun performClick(): Boolean {
        callback?.onBannerClicked(pages[currentPage])
        return true
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
            removeCallbacks(autoSwipeCallback)

            autoSwipeCallbackAdded = false
        }
    }

    private fun startAutoSwipe() {
        if (autoSwipeCallbackAdded) return
        if (!canScroll) return
        postDelayed(autoSwipeCallback, autoSwipeDelay)

        autoSwipeCallbackAdded = true
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

        fun onBannerClicked(page: BannerPageModel)

        fun onBannerClosed(page: BannerPageModel)
    }
}
