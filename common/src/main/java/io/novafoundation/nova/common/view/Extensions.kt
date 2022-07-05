package io.novafoundation.nova.common.view

import android.content.Context
import android.os.CountDownTimer
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private val TIMER_TAG = R.string.common_time_left

@OptIn(ExperimentalTime::class)
fun TextView.startTimer(
    millis: Long,
    millisCalculatedAt: Long? = null,
    @StringRes customMessageFormat: Int? = null,
    onFinish: ((view: TextView) -> Unit)? = null
) {
    val timePassedSinceCalculation = if (millisCalculatedAt != null) System.currentTimeMillis() - millisCalculatedAt else 0L

    val currentTimer = getTag(TIMER_TAG)

    if (currentTimer is CountDownTimer) {
        currentTimer.cancel()
    }

    val newTimer = object : CountDownTimer(millis - timePassedSinceCalculation, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val formattedTime = millisUntilFinished.milliseconds.formatTimer(context)

            val message = customMessageFormat?.let {
                resources.getString(customMessageFormat, formattedTime)
            } ?: formattedTime

            this@startTimer.text = message
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

    newTimer.start()

    setTag(TIMER_TAG, newTimer)
}

@OptIn(ExperimentalTime::class)
private fun Duration.formatTimer(
    context: Context
) = format(
    estimated = false,
    context = context,
    timeFormat = { hours, minutes, seconds -> "%02d:%02d:%02d".format(hours, minutes, seconds) }
)

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

fun <K> Switch.bindFromMapOrHide(key: K, map: Map<out K, MutableStateFlow<Boolean>>, lifecycleScope: LifecycleCoroutineScope) {
    val source = map[key]

    if (source != null) {
        field.bindTo(source, lifecycleScope)

        makeVisible()
    } else {
        makeGone()
    }
}
