package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.item_timeline_default_item.view.*

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
        itemTimelineTitle.text = timelineState.title
        itemTimelineSubtitle.text = timelineState.subtitle
        itemTimelineSubtitle.setTextColorRes(timelineState.subtitleColor)
        itemTimelineSubtitle.setDrawableStart(timelineState.subtitleIconRes, widthInDp = 16, tint = timelineState.subtitleColor, paddingInDp = 4)
    }
}
