package io.novafoundation.nova.common.resources

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import io.novafoundation.nova.common.R
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
interface ResourceManager {

    fun loadRawString(@RawRes res: Int): String

    fun getString(res: Int): String

    fun getString(res: Int, vararg arguments: Any): String

    fun getColor(res: Int): Int

    fun getQuantityString(id: Int, quantity: Int): String
    fun getQuantityString(id: Int, quantity: Int, vararg arguments: Any): String

    fun measureInPx(dp: Int): Int

    fun formatDate(timestamp: Long): String
    fun formatDuration(elapsedTime: Long): String

    fun formatDuration(duration: Duration, estimated: Boolean = true): String

    fun formatTime(timestamp: Long): String

    fun getDrawable(@DrawableRes id: Int): Drawable
}

fun ResourceManager.formatTimeLeft(elapsedTimeInMillis: Long): String {
    val durationFormatted = formatDuration(elapsedTimeInMillis)

    return getString(R.string.common_left, durationFormatted)
}
