package io.novafoundation.nova.common.utils

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.SpannedString
import androidx.annotation.StringRes
import java.util.regex.Pattern

/**
 * The simple version of formatter.
 * Supports formatting only next types: %s, %1$s.
 */
object SpannableFormatter {

    // Add other formatting types in next patterns such as 'd' in [] to extend functionality
    private val FORMAT_SEQUENCE: Pattern = Pattern.compile("%\\d*\\\$?[s]")
    private val INDEX_PATTERN: Pattern = Pattern.compile("(?<=^%)(\\d+)(?=\\\$[s]\$)") // search index in %1$s.

    /**
     * Not throw format exceptions if format is incorrect.
     * In case when format is incorrect will return dirty string with format types.
     */
    fun format(context: Context, @StringRes resId: Int, vararg args: Any): SpannedString {
        val rawString = context.getString(resId)

        return format(rawString, *args)
    }

    /**
     * Not throw format exceptions if format is incorrect.
     * In case when format is incorrect will return dirty string with format types.
     */
    fun format(format: CharSequence, vararg args: Any): SpannedString {
        val out = SpannableStringBuilder(format)
        val matcher = FORMAT_SEQUENCE.matcher(format)
        var i = 0
        while (matcher.find()) {
            matcher.group()
            val argNumber = parseArgNumber(matcher.group()) ?: i
            if (argNumber >= args.size) {
                continue
            }
            val arg = args[argNumber]
            val start = matcher.start()
            val end = matcher.end()
            if (arg is CharSequence) {
                out.replace(start - i, end - i, arg)
            } else {
                out.replace(start - i, end - i, arg.toString())
            }
            i += end - start - arg.toString().length
        }
        return SpannedString(out)
    }

    private fun parseArgNumber(argNumberString: String): Int? {
        val matcher = INDEX_PATTERN.matcher(argNumberString)
        if (matcher.find()) {
            return matcher.group().toInt() - 1
        }

        return null
    }
}
