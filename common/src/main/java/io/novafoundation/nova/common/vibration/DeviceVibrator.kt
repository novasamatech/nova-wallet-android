package io.novafoundation.nova.common.vibration

import android.content.Context
import io.novafoundation.nova.common.utils.vibrate

class DeviceVibrator(
    private val context: Context
) {

    companion object {
        private const val SHORT_VIBRATION_DURATION = 200L
    }

    fun makeShortVibration() {
        context.vibrate(SHORT_VIBRATION_DURATION)
    }
}
