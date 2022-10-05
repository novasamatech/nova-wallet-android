package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import io.novafoundation.nova.feature_governance_impl.R
import java.lang.Float.max
import java.lang.Float.min

class VotesView : View {

    private val positivePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val negativePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thresholdPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var minimumLineLength: Float = 0f
    private var votesStrokeWidth: Float = 0f
    private var cornerRadius: Float = 0f
    private var marginBetweenVotes: Float = 0f
    private var thresholdWidth: Float = 0f
    private var thresholdHeight: Float = 0f
    private var thresholdCornerRadius: Float = 0f

    private var threshold: Float = 0.5f
    private var positivePercentage: Float = 0.5f

    private var positiveRect = RectF()
    private var negativeRect = RectF()
    private var thresholdRect = RectF()
    private var hasPositiveVotes = true
    private var hasNegativeVotes = true

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.VotesView,
            defStyle,
            0
        )

        val positiveColor = a.getColor(R.styleable.VotesView_positiveColor, Color.GREEN)
        val negativeColor = a.getColor(R.styleable.VotesView_negativeColor, Color.RED)
        val thresholdColor = a.getColor(R.styleable.VotesView_thresholdColor, Color.RED)
        val thresholdShadowColor = a.getColor(R.styleable.VotesView_thresholdShadowColor, Color.GRAY)
        val thresholdShadowSize = a.getDimension(R.styleable.VotesView_thresholdShadowSize, 0f)
        votesStrokeWidth = a.getDimension(R.styleable.VotesView_votesStrokeWidth, 2f)
        marginBetweenVotes = a.getDimension(R.styleable.VotesView_marginBetweenVotes, 0f)
        minimumLineLength = a.getDimension(R.styleable.VotesView_minimumLineLength, 0f)
        thresholdWidth = a.getDimension(R.styleable.VotesView_thresholdWidth, 0f)
        thresholdHeight = a.getDimension(R.styleable.VotesView_thresholdHeight, 0f)
        thresholdCornerRadius = a.getDimension(R.styleable.VotesView_thresholdCornerRadius, 0f)

        a.recycle()

        cornerRadius = votesStrokeWidth / 2

        with(positivePaint) {
            color = positiveColor
            strokeWidth = votesStrokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        with(negativePaint) {
            color = negativeColor
            strokeWidth = votesStrokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        with(thresholdPaint) {
            color = thresholdColor
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(thresholdShadowSize, 0f, 0f, thresholdShadowColor)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val percentageArea: Float = measuredWidth - (paddingStart + paddingEnd).toFloat()

        val lineY = measuredHeight / 2f
        val lineTop = lineY + cornerRadius
        val lineBottom = lineY - cornerRadius
        val lineStart = paddingStart.toFloat()
        val lineEnd = (measuredWidth - paddingEnd).toFloat()

        if (hasOnlyPositiveVotes()) {
            positiveRect.set(lineStart, lineTop, lineEnd, lineBottom)
        } else if (hasOnlyNegativeVotes()) {
            negativeRect.set(lineStart, lineTop, lineEnd, lineBottom)
        } else {
            val halfMarginBetweenVotes = marginBetweenVotes / 2
            val positivePercentageWidth: Float = percentageArea * positivePercentage
            val maximumPositivePercentageWidth = percentageArea - halfMarginBetweenVotes - minimumLineLength
            val minimumPositivePercentageWidth = minimumLineLength + halfMarginBetweenVotes
            val stablePositivePercentageWidth: Float = max(min(positivePercentageWidth, maximumPositivePercentageWidth), minimumPositivePercentageWidth)
            val positiveEnd = lineStart + stablePositivePercentageWidth - halfMarginBetweenVotes
            val negativeStart = lineStart + stablePositivePercentageWidth + halfMarginBetweenVotes
            positiveRect.set(lineStart, lineTop, positiveEnd, lineBottom)
            negativeRect.set(negativeStart, lineTop, lineEnd, lineBottom)
        }

        val thresholdPosition = paddingStart + percentageArea * threshold
        val thresholdHalfWidth = thresholdWidth / 2
        val thresholdHalfHeight = thresholdHeight / 2
        thresholdRect.set(
            thresholdPosition - thresholdHalfWidth,
            lineY - thresholdHalfHeight,
            thresholdPosition + thresholdHalfWidth,
            lineY + thresholdHalfHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (hasPositiveVotes) {
            canvas.drawRoundRect(positiveRect, cornerRadius, cornerRadius, positivePaint)
        }

        if (hasNegativeVotes) {
            canvas.drawRoundRect(negativeRect, cornerRadius, cornerRadius, negativePaint)
        }

        canvas.drawRoundRect(thresholdRect, thresholdCornerRadius, thresholdCornerRadius, thresholdPaint)
    }

    fun setPositiveVotesPercentage(@FloatRange(from = 0.0, to = 1.0) positivePercentage: Float) {
        this.positivePercentage = max(min(1f, positivePercentage), 0f)
        hasPositiveVotes = positivePercentage > 0f
        hasNegativeVotes = positivePercentage < 1f
        requestLayout()
    }

    fun setThreshold(@FloatRange(from = 0.0, to = 1.0) threshold: Float) {
        this.threshold = max(min(1f, threshold), 0f)
        requestLayout()
    }

    private fun hasOnlyPositiveVotes(): Boolean {
        return hasPositiveVotes && !hasNegativeVotes
    }

    private fun hasOnlyNegativeVotes(): Boolean {
        return hasNegativeVotes && !hasPositiveVotes
    }
}
