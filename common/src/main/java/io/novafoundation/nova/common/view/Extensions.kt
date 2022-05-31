package io.novafoundation.nova.common.view

import android.os.CountDownTimer
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.format
import kotlinx.coroutines.flow.MutableStateFlow
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
            val formattedTime = millisUntilFinished.milliseconds.format(estimated = true, context)

            val message = customMessageFormat?.let {
                resources.getString(customMessageFormat, formattedTime)
            } ?: formattedTime

            this@startTimer.text = message
        }

        override fun onFinish() {
            if (onFinish != null) {
                onFinish(this@startTimer)
            } else {
                this@startTimer.text = 0L.milliseconds.format(estimated = false, context)
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

fun <K> CompoundButton.bindFromMap(key: K, map: Map<out K, MutableStateFlow<Boolean>>, lifecycleScope: LifecycleCoroutineScope) {
    val source = map[key] ?: error("Cannot find $key source")

    bindTo(source, lifecycleScope)
}
