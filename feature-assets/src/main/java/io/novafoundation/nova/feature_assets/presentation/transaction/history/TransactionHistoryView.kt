package io.novafoundation.nova.feature_assets.presentation.transaction.history

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.graphics.Rect
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.DrawableExtension
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.enableShowingNewlyAddedTopElements
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.updateTopMargin
import io.novafoundation.nova.common.view.bottomSheet.LockBottomSheetBehavior
import io.novafoundation.nova.common.view.shape.getTopRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ViewTransferHistoryBinding
import kotlin.math.max

typealias ScrollingListener = (position: Int) -> Unit
typealias SlidingStateListener = (Int) -> Unit
typealias TransactionClickListener = (transactionId: String) -> Unit

private const val MIN_MARGIN = 20 // dp
private const val MAX_MARGIN = 32 // dp

private const val PULLER_VISIBILITY_OFFSET = 0.9

private const val OFFSET_KEY = "OFFSET"
private const val SUPER_STATE = "SUPER_STATE"

private const val MIN_HEIGHT_DP = 126

class TransferHistorySheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), TransactionHistoryAdapter.Handler {

    private var bottomSheetBehavior: LockBottomSheetBehavior<View>? = null

    private var anchor: View? = null

    private val argbEvaluator = ArgbEvaluator()

    private var scrollingListener: ScrollingListener? = null
    private var slidingStateListener: SlidingStateListener? = null
    private var transactionClickListener: TransactionClickListener? = null

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        if (isInEditMode) {
            ImageLoader.invoke(context)
        } else {
            FeatureUtils.getCommonApi(context).imageLoader()
        }
    }

    private val adapter: TransactionHistoryAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TransactionHistoryAdapter(this, imageLoader)
    }

    private var lastOffset: Float = 0.0F

    private var adapterDataObserver: RecyclerView.AdapterDataObserver? = null

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        anchor?.let {
            bottomSheetBehavior?.peekHeight = max(parentView.measuredHeight - it.bottom, MIN_HEIGHT_DP.dp)
        }
    }

    private val collapsedBackgroundColor: Int = context.getColor(R.color.android_history_background)
    private val expandedBackgroundColor: Int = context.getColor(R.color.secondary_screen_background)

    private val binder = ViewTransferHistoryBinding.inflate(inflater(), this)

    init {
        val contentBackgroundDrawable = context.getTopRoundedCornerDrawable(
            fillColorRes = R.color.android_history_background,
            strokeColorRes = R.color.container_border,
            cornerSizeInDp = 16
        )

        // Extend background drawable from left and right to make stroke in background on sides hidden
        val borderDrawable = DrawableExtension(
            contentDrawable = contentBackgroundDrawable,
            extensionOffset = Rect(1.dp(context), 0, 1.dp(context), 0),
        )

        background = borderDrawable

        binder.transactionHistoryList.adapter = adapter
        binder.transactionHistoryList.setHasFixedSize(true)

        addScrollListener()

        updateSlidingEffects()
    }

    fun setFiltersVisible(visible: Boolean) {
        binder.transactionHistoryFilter.setVisible(visible)
    }

    fun showProgress() {
        binder.placeholder.makeGone()
        binder.transactionHistoryProgress.makeVisible()
        binder.transactionHistoryList.makeGone()

        adapter.submitList(emptyList())

        bottomSheetBehavior?.isDraggable = false
    }

    fun showPlaceholder() {
        binder.placeholder.makeVisible()
        binder.transactionHistoryProgress.makeGone()
        binder.transactionHistoryList.makeGone()

        adapter.submitList(emptyList())

        bottomSheetBehavior?.isDraggable = true
    }

    fun showTransactions(transactions: List<Any>) {
        binder.placeholder.makeGone()
        binder.transactionHistoryProgress.makeGone()
        binder.transactionHistoryList.makeVisible()

        bottomSheetBehavior?.isDraggable = true

        adapter.submitList(transactions)
    }

    fun setScrollingListener(listener: ScrollingListener) {
        scrollingListener = listener
    }

    fun setSlidingStateListener(listener: SlidingStateListener) {
        slidingStateListener = listener
    }

    fun setTransactionClickListener(listener: TransactionClickListener) {
        transactionClickListener = listener
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()

        return Bundle().apply {
            putParcelable(SUPER_STATE, superState)
            putFloat(OFFSET_KEY, lastOffset)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state[SUPER_STATE] as Parcelable)

            lastOffset = state.getFloat(OFFSET_KEY)
            updateSlidingEffects()
        }

        bottomSheetBehavior?.state?.let {
            slidingStateListener?.invoke(it)
        }
    }

    fun setFilterClickListener(clickListener: OnClickListener) {
        binder.transactionHistoryFilter.setOnClickListener(clickListener)
    }

    fun initializeBehavior(anchorView: View) {
        anchor = anchorView

        bottomSheetBehavior = LockBottomSheetBehavior.fromView(this)

        bottomSheetBehavior!!.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                lastOffset = slideOffset

                updateSlidingEffects()
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                slidingStateListener?.invoke(newState)
            }
        })

        addLayoutListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        adapterDataObserver = binder.transactionHistoryList.enableShowingNewlyAddedTopElements()
    }

    override fun onDetachedFromWindow() {
        removeLayoutListener()

        adapter.unregisterAdapterDataObserver(adapterDataObserver!!)

        super.onDetachedFromWindow()
    }

    override fun transactionClicked(transactionId: String) {
        transactionClickListener?.invoke(transactionId)
    }

    private fun addScrollListener() {
        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                scrollingListener?.invoke(lastVisiblePosition)
            }
        }

        binder.transactionHistoryList.addOnScrollListener(scrollListener)
    }

    private fun removeLayoutListener() {
        parentView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }

    private fun addLayoutListener() {
        parentView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun updateSlidingEffects() {
        updateBackgroundAlpha()

        updateTitleMargin()

        updatePullerVisibility()

        requestLayout()
    }

    private fun updateTitleMargin() {
        val newMargin = linearUpdate(MIN_MARGIN, MAX_MARGIN, lastOffset)
        val newMarginPx = newMargin.dp(context)

        binder.transactionHistoryTitle.updateTopMargin(newMarginPx)
    }

    private fun updatePullerVisibility() {
        binder.transactionHistoryPuller.setVisible(lastOffset < PULLER_VISIBILITY_OFFSET, falseState = View.INVISIBLE)
    }

    private fun updateBackgroundAlpha() {
        val backgroundColor = argbEvaluator.evaluate(lastOffset, collapsedBackgroundColor, expandedBackgroundColor) as Int

        backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    private val parentView: View
        get() = parent as View

    private fun linearUpdate(min: Int, max: Int, progress: Float): Int {
        return (min + (max - min) * progress).toInt()
    }
}
