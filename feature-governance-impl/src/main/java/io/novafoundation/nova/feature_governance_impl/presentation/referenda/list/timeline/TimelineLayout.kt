package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import kotlin.math.roundToInt

class TimelineLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val timelinePointPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timelinePathPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timelineUnfinishedPathPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dashFilledInterval: Float = 2.dpF(context)
    private val dashEmptyInterval: Float = 3.dpF(context)

    private var timeline: Timeline = createDefaultTimeline()

    private var statePointSize: Float = 0f
    private var pointToStrokeOffset: Float = 0f
    private var halfStatePointSize: Float = 0f
    private var itemStartPadding: Int = 0

    private var timelineStatePoints: List<PointF> = listOf()
    private var timelineStatePath: Path = Path()
    private val timelineFinishedPath: Path = Path()

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.TimelineLayout,
            defStyleAttr,
            0
        )

        val timelineDefaultColor = a.getColor(R.styleable.TimelineLayout_timelineDefaultColor, Color.GRAY)
        val timelineUnfinishedColor = a.getColor(R.styleable.TimelineLayout_timelineUnfinishedColor, Color.GRAY)
        val strokeWidth = a.getDimension(R.styleable.TimelineLayout_timelineStrokeWidth, 1.dpF(context))
        val itemPaddingStartToPoint = a.getDimension(R.styleable.TimelineLayout_timelineItemStartPadding, 1.dpF(context)).roundToInt()
        pointToStrokeOffset = a.getDimension(R.styleable.TimelineLayout_timelinePointToStrokeOffset, 1.dpF(context))
        statePointSize = a.getDimension(R.styleable.TimelineLayout_timelineItemStatePointSize, 4.dpF(context))

        a.recycle()

        itemStartPadding = (itemPaddingStartToPoint + statePointSize).roundToInt()
        halfStatePointSize = statePointSize / 2f

        with(timelinePointPaint) {
            color = timelineDefaultColor
            style = Paint.Style.FILL
        }
        with(timelinePathPaint) {
            color = timelineDefaultColor
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
        }
        with(timelineUnfinishedPathPaint) {
            color = timelineUnfinishedColor
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
            val dashIntervals = floatArrayOf(dashFilledInterval, dashEmptyInterval)
            setPathEffect(DashPathEffect(dashIntervals, 0f))
        }

        orientation = VERTICAL

        setWillNotDraw(false)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        val halfStatePointSize = statePointSize / 2
        val centerOfStatePointX = paddingStart + halfStatePointSize
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val timelineItem = childView as TimelineItem
            val centerOfStatePointY = childView.y + timelineItem.getDrawPointOffset()
            timelineStatePoints[i].set(centerOfStatePointX, centerOfStatePointY)
        }

        val totalOffsetFromPointCenter = halfStatePointSize + pointToStrokeOffset

        timelineStatePath.reset()
        if (timelineStatePoints.size > 1) {
            val pathsCount = timeline.states.size - 1
            for (i in 0 until pathsCount) {
                val pointStart = timelineStatePoints[i]
                val pointEnd = timelineStatePoints[i + 1]
                timelineStatePath.moveTo(pointStart.x, pointStart.y + totalOffsetFromPointCenter)
                timelineStatePath.lineTo(pointEnd.x, pointEnd.y - totalOffsetFromPointCenter)
            }
        }

        timelineFinishedPath.reset()
        if (!timeline.finished && timelineStatePoints.isNotEmpty()) {
            val lastPoint = timelineStatePoints.last()
            timelineFinishedPath.moveTo(lastPoint.x, lastPoint.y + totalOffsetFromPointCenter)
            timelineFinishedPath.lineTo(lastPoint.x, measuredHeight.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        timelineStatePoints.forEach {
            canvas.drawCircle(it.x, it.y, halfStatePointSize, timelinePointPaint)
        }
        canvas.drawPath(timelineStatePath, timelinePathPaint)
        canvas.drawPath(timelineFinishedPath, timelineUnfinishedPathPaint)
    }

    fun setTimeline(timeline: Timeline) {
        this.timeline = timeline
        removeAllViewsInLayout()
        timeline.states.forEach { timelineState ->
            val timelineItem = createTimelineDefaultItem(timelineState)
            timelineItem.updatePadding(start = itemStartPadding)
            addView(timelineItem)
        }
        timelineStatePoints = List(timeline.states.size) { PointF() }

        requestLayout()
    }

    private fun createTimelineDefaultItem(timelineState: TimelineState): TimelineItem {
        val timeLineItem = TimelineItem(context)
        timeLineItem.setTimelineState(timelineState)
        return timeLineItem
    }

    private fun createDefaultTimeline(): Timeline {
        return Timeline(
            listOf(),
            true
        )
    }

    class Timeline(
        val states: List<TimelineState>,
        val finished: Boolean
    )

    sealed class TimelineState {

        class Historical(
            val title: String,
            val subtitle: String?,
        ) : TimelineState()

        class Current(
            val title: String,
            val subtitle: ReferendumTimeEstimation
        ) : TimelineState()
    }
}
