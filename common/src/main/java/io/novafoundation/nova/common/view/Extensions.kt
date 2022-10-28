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
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.onDestroy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private val TIMER_TAG = R.string.common_time_left

fun TextView.startTimer(
    value: TimerValue,
    @StringRes customMessageFormat: Int? = null,
    lifecycle: Lifecycle? = null,
    onTick: ((view: TextView, millisUntilFinished: Long) -> Unit)? = null,
    onFinish: ((view: TextView) -> Unit)? = null
) = startTimer(value.millis, value.millisCalculatedAt, lifecycle, customMessageFormat, onTick, onFinish)

@OptIn(ExperimentalTime::class)
fun TextView.startTimer(
    millis: Long,
    millisCalculatedAt: Long? = null,
    lifecycle: Lifecycle? = null,
    @StringRes customMessageFormat: Int? = null,
    onTick: ((view: TextView, millisUntilFinished: Long) -> Unit)? = null,
    onFinish: ((view: TextView) -> Unit)? = null
) {
    val timePassedSinceCalculation = if (millisCalculatedAt != null) System.currentTimeMillis() - millisCalculatedAt else 0L

    val currentTimer = getTag(TIMER_TAG)

    if (currentTimer is CountDownTimer) {
        currentTimer.cancel()
    }
    val formattedTime = millis.milliseconds.formatTimer(context)

    val message = customMessageFormat?.let {
        resources.getString(customMessageFormat, formattedTime)
    } ?: formattedTime

    this@startTimer.text = message

    val newTimer = object : CountDownTimer(millis - timePassedSinceCalculation, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            setNewValue(millisUntilFinished, customMessageFormat)

            onTick?.invoke(this@startTimer, millisUntilFinished)
        }

        override fun onFinish() {
            if (onFinish != null) {
                onFinish(this@startTimer)
            } else {
                this@startTimer.text = 0L.milliseconds.formatTimer(context)
            }

            cancel()

            setTag(TIMER_TAG, null)
        }
    }

    lifecycle?.onDestroy {
        newTimer.cancel()
    }

    setNewValue(millis - timePassedSinceCalculation, customMessageFormat)
    newTimer.start()

    setTag(TIMER_TAG, newTimer)
}

private fun Duration.formatTimer(
    context: Context
) = format(
    estimated = false,
    context = context,
    timeFormat = { hours, minutes, seconds -> "%02d:%02d:%02d".format(hours, minutes, seconds) }
)

@OptIn(ExperimentalTime::class)
private fun TextView.setNewValue(mills: Long, timeFormatRes: Int?) {
    val formattedTime = mills.milliseconds.formatTimer(context)

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
    val source = map[key] ?: error("Cannot find $key source")

    bindTo(source, lifecycleScope)
}
