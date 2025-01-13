package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_governance_impl.databinding.ItemTimelineDefaultItemBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation

class TimelineItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binder = ItemTimelineDefaultItemBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
    }

    fun getDrawPointOffset(): Float {
        return binder.itemTimelineTitle.y + binder.itemTimelineTitle.measuredHeight / 2f
    }

    fun setTimelineState(timelineState: TimelineLayout.TimelineState) {
        binder.itemTimelineSubtitle.stopTimer()

        when (timelineState) {
            is TimelineLayout.TimelineState.Historical -> {
                binder.itemTimelineTitle.text = timelineState.title
                binder.itemTimelineSubtitle.text = timelineState.subtitle
            }
            is TimelineLayout.TimelineState.Current -> {
                binder.itemTimelineTitle.text = timelineState.title
                binder.itemTimelineSubtitle.setReferendumTimeEstimation(timelineState.subtitle, Gravity.START)
            }
        }
    }
}
