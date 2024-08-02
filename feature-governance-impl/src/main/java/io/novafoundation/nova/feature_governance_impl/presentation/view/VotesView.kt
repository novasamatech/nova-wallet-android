package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import io.novafoundation.nova.feature_governance_impl.R
import java.lang.Float.max
import java.lang.Float.min

class VotesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val noVotesPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val positivePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val negativePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thresholdPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var minimumLineLength: Float = 0f
    private var votesLineWidth: Float = 0f
    private var cornerRadius: Float = 0f
    private var marginBetweenVotes: Float = 0f
    private var thresholdWidth: Float = 0f
    private var thresholdHeight: Float = 0f
    private var thresholdCornerRadius: Float = 0f

    private var threshold: Float = 0.5f
    private var positiveFraction: Float = 0.0f

    private var noVotesRect = RectF()
    private var positiveRect = RectF()
    private var negativeRect = RectF()
    private var thresholdRect = RectF()
    private var hasPositiveVotes = false
    private var hasNegativeVotes = false

    private var thresholdVisible = true

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.VotesView,
            defStyleAttr,
            0
        )

        val noVotesColor = a.getColor(R.styleable.VotesView_noVotesColor, Color.GRAY)
        val positiveColor = a.getColor(R.styleable.VotesView_positiveColor, Color.GREEN)
        val negativeColor = a.getColor(R.styleable.VotesView_negativeColor, Color.RED)
        val thresholdColor = a.getColor(R.styleable.VotesView_thresholdColor, Color.RED)
        val thresholdShadowColor = a.getColor(R.styleable.VotesView_thresholdShadowColor, Color.GRAY)
        val thresholdShadowSize = a.getDimension(R.styleable.VotesView_thresholdShadowSize, 0f)
        votesLineWidth = a.getDimension(R.styleable.VotesView_votesLineWidth, 2f)
        marginBetweenVotes = a.getDimension(R.styleable.VotesView_marginBetweenVotes, 0f)
        minimumLineLength = a.getDimension(R.styleable.VotesView_minimumLineLength, 0f)
        thresholdWidth = a.getDimension(R.styleable.VotesView_thresholdWidth, 0f)
        thresholdHeight = a.getDimension(R.styleable.VotesView_thresholdHeight, 0f)
        thresholdCornerRadius = a.getDimension(R.styleable.VotesView_thresholdCornerRadius, 0f)

        a.recycle()

        cornerRadius = votesLineWidth / 2

        noVotesPaint.color = noVotesColor
        positivePaint.color = positiveColor
        negativePaint.color = negativeColor

        with(thresholdPaint) {
            color = thresholdColor
            setShadowLayer(thresholdShadowSize, 0f, 0f, thresholdShadowColor)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val percentageArea: Float = measuredWidth - (paddingStart + paddingEnd).toFloat()

        val lineY = measuredHeight / 2f
        val lineTop = lineY + cornerRadius
        val lineBottom = lineY - cornerRadius
        val lineStart = paddingStart.toFloat()
        val lineEnd = (measuredWidth - paddingEnd).toFloat()

        if (noVotes()) {
            noVotesRect.set(lineStart, lineTop, lineEnd, lineBottom)
        } else if (hasOnlyPositiveVotes()) {
            positiveRect.set(lineStart, lineTop, lineEnd, lineBottom)
        } else if (hasOnlyNegativeVotes()) {
            negativeRect.set(lineStart, lineTop, lineEnd, lineBottom)
        } else {
            val halfMarginBetweenVotes = marginBetweenVotes / 2
            val positivePercentageWidth: Float = percentageArea * positiveFraction
            val maximumPositivePercentageWidth = percentageArea - halfMarginBetweenVotes - minimumLineLength
            val minimumPositivePercentageWidth = minimumLineLength + halfMarginBetweenVotes
            val stablePositivePercentageWidth: Float = max(min(positivePercentageWidth, maximumPositivePercentageWidth), minimumPositivePercentageWidth)
            val positiveEnd = lineStart + stablePositivePercentageWidth - halfMarginBetweenVotes
            val negativeStart = lineStart + stablePositivePercentageWidth + halfMarginBetweenVotes
            positiveRect.set(lineStart, lineTop, positiveEnd, lineBottom)
            negativeRect.set(negativeStart, lineTop, lineEnd, lineBottom)
        }

        val thresholdHalfWidth = thresholdWidth / 2
        val thresholdHalfHeight = thresholdHeight / 2
        val minThresholdPosition = paddingStart + thresholdHalfWidth
        val maxThresholdPosition = measuredWidth - paddingEnd - thresholdHalfWidth

        val thresholdPosition = paddingStart + percentageArea * threshold
        val validThresholdPosition = thresholdPosition.coerceIn(minThresholdPosition, maxThresholdPosition)
        thresholdRect.set(
            validThresholdPosition - thresholdHalfWidth,
            lineY - thresholdHalfHeight,
            validThresholdPosition + thresholdHalfWidth,
            lineY + thresholdHalfHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (noVotes()) {
            canvas.drawRoundRect(noVotesRect, cornerRadius, cornerRadius, noVotesPaint)
        } else {
            if (hasPositiveVotes) {
                canvas.drawRoundRect(positiveRect, cornerRadius, cornerRadius, positivePaint)
            }

            if (hasNegativeVotes) {
                canvas.drawRoundRect(negativeRect, cornerRadius, cornerRadius, negativePaint)
            }
        }

        if (thresholdVisible) {
            canvas.drawRoundRect(thresholdRect, thresholdCornerRadius, thresholdCornerRadius, thresholdPaint)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(
            super.onSaveInstanceState(),
            positiveFraction,
            threshold,
            hasPositiveVotes,
            hasNegativeVotes
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            positiveFraction = state.positiveFraction
            threshold = state.threshold
            hasPositiveVotes = state.hasPositiveVotes
            hasNegativeVotes = state.hasNegativeVotes
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    fun setPositiveVotesFraction(positiveFraction: Float?) {
        if (positiveFraction == null) {
            hasPositiveVotes = false
            hasNegativeVotes = false
        } else {
            require(positiveFraction in 0f..1f)
            this.positiveFraction = positiveFraction
            hasPositiveVotes = positiveFraction > 0f
            hasNegativeVotes = positiveFraction < 1f
        }
        requestLayout()
    }

    fun setThreshold(@FloatRange(from = 0.0, to = 1.0) threshold: Float?) {
        thresholdVisible = threshold != null

        if (threshold != null) {
            require(threshold in 0f..1f)
            this.threshold = threshold
        }

        requestLayout()
    }

    private fun hasOnlyPositiveVotes(): Boolean {
        return hasPositiveVotes && !hasNegativeVotes
    }

    private fun hasOnlyNegativeVotes(): Boolean {
        return hasNegativeVotes && !hasPositiveVotes
    }

    private fun noVotes(): Boolean {
        return !hasNegativeVotes && !hasPositiveVotes
    }

    private class SavedState : BaseSavedState {

        val positiveFraction: Float
        val threshold: Float
        val hasPositiveVotes: Boolean
        val hasNegativeVotes: Boolean

        constructor(
            superState: Parcelable?,
            positivePercentage: Float,
            threshold: Float,
            hasPositiveVotes: Boolean,
            hasNegativeVotes: Boolean
        ) : super(superState) {
            this.positiveFraction = positivePercentage
            this.threshold = threshold
            this.hasPositiveVotes = hasPositiveVotes
            this.hasNegativeVotes = hasNegativeVotes
        }

        constructor(parcel: Parcel) : this(parcel, null)

        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            this.positiveFraction = parcel.readFloat()
            this.threshold = parcel.readFloat()
            this.hasPositiveVotes = parcel.readInt() == 1
            this.hasNegativeVotes = parcel.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(positiveFraction)
            out.writeFloat(threshold)
            out.writeInt(if (hasPositiveVotes) 1 else 0)
            out.writeInt(if (hasNegativeVotes) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : ClassLoaderCreator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun createFromParcel(parcel: Parcel, classLoader: ClassLoader?): SavedState {
                return SavedState(parcel, classLoader)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
