package io.novafoundation.nova.common.view

import android.content.Context
import android.os.CountDownTimer
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.formatting.duration.CompoundDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DayAndHourDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DayDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.HoursDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.RoundMinutesDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.TimeDurationFormatter
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.duration.ZeroDurationFormatter
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.onDestroy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

private val TIMER_TAG = R.string.common_time_left

fun TextView.startTimer(
    value: TimerValue,
    @StringRes customMessageFormat: Int? = null,
    lifecycle: Lifecycle? = null,
    timerDurationFormatter: DurationFormatter? = null,
    onTick: ((view: TextView, millisUntilFinished: Long) -> Unit)? = null,
    onFinish: ((view: TextView) -> Unit)? = null
) = startTimer(value.millis, value.millisCalculatedAt, lifecycle, customMessageFormat, timerDurationFormatter, onTick, onFinish)

@OptIn(ExperimentalTime::class)
fun TextView.startTimer(
    millis: Long,
    millisCalculatedAt: Long? = null,
    lifecycle: Lifecycle? = null,
    @StringRes customMessageFormat: Int? = null,
    timerDurationFormatter: DurationFormatter? = null,
    onTick: ((view: TextView, millisUntilFinished: Long) -> Unit)? = null,
    onFinish: ((view: TextView) -> Unit)? = null
) {
    val durationFormatter = timerDurationFormatter ?: getTimerDurationFormatter(context)

    val timePassedSinceCalculation = if (millisCalculatedAt != null) System.currentTimeMillis() - millisCalculatedAt else 0L

    val currentTimer = getTag(TIMER_TAG)

    if (currentTimer is CountDownTimer) {
        currentTimer.cancel()
    }

    val newTimer = object : CountDownTimer(millis - timePassedSinceCalculation, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            setNewValue(durationFormatter, millisUntilFinished, customMessageFormat)

            onTick?.invoke(this@startTimer, millisUntilFinished)
        }

        override fun onFinish() {
            if (onFinish != null) {
                onFinish(this@startTimer)
            } else {
                this@startTimer.text = durationFormatter.format(0L.milliseconds)
            }

            cancel()

            setTag(TIMER_TAG, null)
        }
    }

    lifecycle?.onDestroy {
        newTimer.cancel()
    }

    setNewValue(durationFormatter, millis - timePassedSinceCalculation, customMessageFormat)
    newTimer.start()

    setTag(TIMER_TAG, newTimer)
}

private fun getTimerDurationFormatter(context: Context): DurationFormatter {
    val timeDurationFormatter = TimeDurationFormatter()
    val compoundFormatter = CompoundDurationFormatter(
        DayAndHourDurationFormatter(
            dayFormatter = DayDurationFormatter(context),
            hoursFormatter = HoursDurationFormatter(context)
        ),
        timeDurationFormatter,
        ZeroDurationFormatter(timeDurationFormatter)
    )

    return RoundMinutesDurationFormatter(compoundFormatter, roundMinutesThreshold = 1.days)
}

private fun TextView.setNewValue(durationFormatter: DurationFormatter, mills: Long, timeFormatRes: Int?) {
    val formattedTime = durationFormatter.format(mills.milliseconds)

    val message = timeFormatRes?.let {
        resources.getString(timeFormatRes, formattedTime)
    } ?: formattedTime

    this.text = message
}

fun TextView.stopTimer() {
    val currentTimer = getTag(TIMER_TAG)

    if (currentTimer is CountDownTimer) {
        currentTimer.cancel()
        setTag(TIMER_TAG, null)
    }
}

fun <K> CompoundButton.bindFromMap(key: K, map: Map<out K, MutableStateFlow<Boolean>>, lifecycleScope: LifecycleCoroutineScope) {
    val source = map[key]

    if (source == null) {
        makeGone()
        return
    }

    bindTo(source, lifecycleScope)
}
