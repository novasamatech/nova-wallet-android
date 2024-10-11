package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation

class TimelineItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.item_timeline_default_item, this)
    }

    fun getDrawPointOffset(): Float {
        return itemTimelineTitle.y + itemTimelineTitle.measuredHeight / 2f
    }

    fun setTimelineState(timelineState: TimelineLayout.TimelineState) {
        itemTimelineSubtitle.stopTimer()

        when (timelineState) {
            is TimelineLayout.TimelineState.Historical -> {
                itemTimelineTitle.text = timelineState.title
                itemTimelineSubtitle.text = timelineState.subtitle
            }
            is TimelineLayout.TimelineState.Current -> {
                itemTimelineTitle.text = timelineState.title
                itemTimelineSubtitle.setReferendumTimeEstimation(timelineState.subtitle, Gravity.START)
            }
        }
    }
}
