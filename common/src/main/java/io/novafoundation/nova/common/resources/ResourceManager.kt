package io.novafoundation.nova.common.resources

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.formatting.format
import kotlin.time.Duration

interface ResourceManager {

    fun loadRawString(@RawRes res: Int): String

    fun getString(res: Int): String

    fun getString(res: Int, vararg arguments: Any): String

    fun getText(res: Int): CharSequence

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

    fun getDimensionPixelSize(id: Int): Int

    fun getFont(@FontRes fontRes: Int): Typeface?
}

fun ResourceManager.formatTimeLeft(elapsedTimeInMillis: Long): String {
    val durationFormatted = formatDuration(elapsedTimeInMillis)

    return getString(R.string.common_left, durationFormatted)
}

fun ResourceManager.formatListPreview(
    elements: List<String>,
    maxPreviewItems: Int = 1,
    @StringRes zeroLabel: Int? = R.string.common_none,
): String {
    return when {
        elements.isEmpty() -> zeroLabel?.let(::getString).orEmpty()
        elements.size <= maxPreviewItems -> elements.joinPreviewItems(maxPreviewItems)
        else -> {
            val previewItems = elements.joinPreviewItems(maxPreviewItems)
            val remainingCount = elements.size - maxPreviewItems

            getString(R.string.common_element_and_more_format, previewItems, remainingCount.format())
        }
    }
}

fun ResourceManager.mapBooleanToState(isEnabled: Boolean): String {
    return if (isEnabled) {
        getString(R.string.common_on)
    } else {
        getString(R.string.common_off)
    }
}

private fun List<String>.joinPreviewItems(previewItemsCount: Int): String = take(previewItemsCount).joinToString(separator = ", ")
