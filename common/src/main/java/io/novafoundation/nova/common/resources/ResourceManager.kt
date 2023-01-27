package io.novafoundation.nova.common.resources

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.formatting.format
import kotlin.time.Duration

interface ResourceManager {

    fun loadRawString(@RawRes res: Int): String

    fun getString(res: Int): String

    fun getString(res: Int, vararg arguments: Any): String

    fun getColor(res: Int): Int

    fun getQuantityString(id: Int, quantity: Int): String
    fun getQuantityString(id: Int, quantity: Int, vararg arguments: Any): String

    fun measureInPx(dp: Int): Int

    fun formatDateTime(timestamp: Long): String
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


fun ResourceManager.formatListPreview(
    elements: List<String>,
    @StringRes zeroLabel: Int? = null,
): String {
    return when {
        elements.isEmpty() -> zeroLabel?.let(::getString).orEmpty()
        elements.size == 1 -> elements.single()
        else -> {
            val previewItem = elements.first()
            val remainingCount = elements.size - 1

            getString(R.string.common_element_and_more_format, previewItem, remainingCount.format())
        }
    }
}
