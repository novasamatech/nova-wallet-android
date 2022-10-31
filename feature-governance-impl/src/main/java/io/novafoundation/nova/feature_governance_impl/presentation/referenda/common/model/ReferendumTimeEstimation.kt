package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer

typealias ReferendumTimeEstimationStyleRefresher = () -> ReferendumTimeEstimation.TextStyle

sealed class ReferendumTimeEstimation {

    data class TextStyle(
        @DrawableRes val iconRes: Int,
        @ColorRes val colorRes: Int
    ) {
        companion object
    }

    class Timer(
        val time: TimerValue,
        @StringRes val timeFormat: Int,
        val textStyleRefresher: ReferendumTimeEstimationStyleRefresher,
    ) : ReferendumTimeEstimation() {

        override fun hashCode(): Int {
            var result = time.millis.hashCode()
            result = 31 * result + timeFormat
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Timer) return false
            return time.millis == other.time.millis &&
                timeFormat == other.timeFormat
        }
    }

    data class Text(
        val text: String,
        val textStyle: TextStyle
    ) : ReferendumTimeEstimation()
}

fun TextView.setReferendumTimeEstimation(maybeTimeEstimation: ReferendumTimeEstimation?) = letOrHide(maybeTimeEstimation) { timeEstimation ->
    when (timeEstimation) {
        is ReferendumTimeEstimation.Text -> {
            stopTimer()

            text = timeEstimation.text
            setReferendumTextStyle(timeEstimation.textStyle)
        }

        is ReferendumTimeEstimation.Timer -> {
            setReferendumTextStyle(timeEstimation.textStyleRefresher())

            startTimer(
                value = timeEstimation.time,
                customMessageFormat = timeEstimation.timeFormat,
                onTick = { view, _ ->
                    view.setReferendumTextStyle(timeEstimation.textStyleRefresher())
                }
            )
        }
    }
}

private fun TextView.setReferendumTextStyle(textStyle: ReferendumTimeEstimation.TextStyle) {
    setTextColorRes(textStyle.colorRes)
    setDrawableEnd(textStyle.iconRes, widthInDp = 16, paddingInDp = 4, tint = textStyle.colorRes)
}
