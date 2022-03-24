package io.novafoundation.nova.common.view

import android.os.CountDownTimer
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.bindTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private val TIMER_TAG = R.string.common_time_left

@OptIn(ExperimentalTime::class)
fun TextView.startTimer(
    millis: Long,
    millisCalculatedAt: Long? = null,
    @PluralsRes daysPlurals: Int = R.plurals.staking_payouts_days_left,
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
            val days = millisUntilFinished.milliseconds.inDays.toInt()

            val formattedTime = if (days > 0) {
                resources.getQuantityString(daysPlurals, days, days)
            } else {
                millisUntilFinished.formatTime()
            }

            val message = customMessageFormat?.let {
                resources.getString(customMessageFormat, formattedTime)
            } ?: formattedTime

            this@startTimer.text = message
        }

        override fun onFinish() {
            if (onFinish != null) {
                onFinish(this@startTimer)
            } else {
                this@startTimer.text = 0L.formatTime()
            }

            cancel()

            setTag(TIMER_TAG, null)
        }
    }

    newTimer.start()

    setTag(TIMER_TAG, newTimer)
}

fun TextView.stopTimer() {
    val currentTimer = getTag(TIMER_TAG)

    if (currentTimer is CountDownTimer) {
        currentTimer.cancel()
        setTag(TIMER_TAG, null)
    }
}

fun Long.formatTime(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun <K> CompoundButton.bindFromMap(key: K, map: Map<out K, MutableStateFlow<Boolean>>, lifecycleScope: LifecycleCoroutineScope) {
    val source = map[key] ?: error("Cannot find $key source")

    bindTo(source, lifecycleScope)
}
