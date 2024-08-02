package io.novafoundation.nova.common.utils.formatting.spannable

import android.text.SpannedString
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter.fill
import java.util.regex.Pattern

/**
 * The simple version of formatter.
 * Supports formatting only next types: %s, %1$s.
 */
object SpannableFormatter {

    // Add other formatting types in next patterns such as 'd' in [] to extend functionality
    private val FORMAT_SEQUENCE: Pattern = Pattern.compile("%\\d*\\\$?[s]")
    private val INDEX_PATTERN: Pattern = Pattern.compile("(?<=^%)(\\d+)(?=\\\$[s]\$)") // search index in %1$s.

    fun format(format: CharSequence, vararg args: Any): SpannedString {
        val formattedResult = fill(SpannableFormatterBuilder(format), *args)
        return SpannedString(formattedResult)
    }

    /**
     * Not throw format exceptions if format is incorrect.
     * In case when format is incorrect will return dirty string with format types.
     */
    fun fill(builder: Builder, vararg args: Any): CharSequence {
        val matcher = FORMAT_SEQUENCE.matcher(builder.format)
        var index = 0
        var offset = 0
        while (matcher.find()) {
            matcher.group()
            val argNumber = parseArgNumber(matcher.group()) ?: index
            if (argNumber >= args.size) {
                continue
            }
            val arg = args[argNumber]
            val start = matcher.start() - offset
            val end = matcher.end() - offset
            if (arg is CharSequence) {
                builder.replace(start, end, arg)
            } else {
                builder.replace(start, end, arg.toString())
            }
            index++
            offset += end - start - arg.toString().length
        }
        return builder.result()
    }

    private fun parseArgNumber(argNumberString: String): Int? {
        val matcher = INDEX_PATTERN.matcher(argNumberString)
        if (matcher.find()) {
            return matcher.group().toInt() - 1
        }

        return null
    }

    interface Builder {

        val format: CharSequence

        fun replace(start: Int, end: Int, text: CharSequence)

        fun result(): CharSequence
    }
}
