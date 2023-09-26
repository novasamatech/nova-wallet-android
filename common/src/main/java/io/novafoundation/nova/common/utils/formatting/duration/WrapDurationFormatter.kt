package io.novafoundation.nova.common.utils.formatting.duration

import android.content.Context
import androidx.annotation.StringRes
import kotlin.time.Duration

class WrapDurationFormatter(
    private val context: Context,
    @StringRes private val resId: Int,
    private val nestedFormatter: BoundedDurationFormatter
) : BoundedDurationFormatter {

    override val threshold: Duration = nestedFormatter.threshold

    override fun format(duration: Duration): String {
        val nestedFormatterString = nestedFormatter.format(duration)
        return context.getString(resId, nestedFormatterString)
    }
}

fun BoundedDurationFormatter.wrapInto(context: Context, @StringRes prefixRes: Int): WrapDurationFormatter {
    return WrapDurationFormatter(context, prefixRes, this)
}
