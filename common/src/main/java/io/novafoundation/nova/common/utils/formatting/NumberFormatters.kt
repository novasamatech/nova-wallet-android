package io.novafoundation.nova.common.utils.formatting

import android.content.Context
import android.text.format.DateUtils
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.daysFromMillis
import io.novafoundation.nova.common.utils.fractionToPercentage
import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

const val DATE_ISO_8601_FULL = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
const val DATE_ISO_8601_NO_MS = "yyyy-MM-dd'T'HH:mm:ss'Z'"

private const val DECIMAL_PATTERN_BASE = "###,###."

private const val GROUPING_SEPARATOR = ','
private const val DECIMAL_SEPARATOR = '.'

private const val FULL_PRECISION = 5
const val ABBREVIATED_PRECISION = 2

private val dateTimeFormat = SimpleDateFormat.getDateTimeInstance()
private val dateTimeFormatISO_8601 by lazy { SimpleDateFormat(DATE_ISO_8601_FULL, Locale.getDefault()) }
private val dateTimeFormatISO_8601_NoMs by lazy { SimpleDateFormat(DATE_ISO_8601_NO_MS, Locale.getDefault()) }

private val defaultAbbreviationFormatter = FixedPrecisionFormatter(ABBREVIATED_PRECISION)
private val defaultFullFormatter = FixedPrecisionFormatter(FULL_PRECISION)

private val thousandAbbreviation = NumberAbbreviation(
    threshold = BigDecimal("1E+3"),
    divisor = BigDecimal.ONE,
    suffix = "",
    formatter = defaultAbbreviationFormatter
)

private val millionAbbreviation = NumberAbbreviation(
    threshold = BigDecimal("1E+6"),
    divisor = BigDecimal("1E+6"),
    suffix = "M",
    formatter = defaultAbbreviationFormatter
)

private val billionAbbreviation = NumberAbbreviation(
    threshold = BigDecimal("1E+9"),
    divisor = BigDecimal("1E+9"),
    suffix = "B",
    formatter = defaultAbbreviationFormatter
)

private val trillionAbbreviation = NumberAbbreviation(
    threshold = BigDecimal("1E+12"),
    divisor = BigDecimal("1E+12"),
    suffix = "T",
    formatter = defaultAbbreviationFormatter
)

private val defaultNumberFormatter = defaultNumberFormatter()

fun BigDecimal.format(roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return defaultNumberFormatter.format(this, roundingMode)
}

fun Int.format(): String {
    return defaultNumberFormatter.format(BigDecimal(this))
}

fun BigDecimal.toAmountInput(): String {
    return toDouble().toString()
}

fun BigInteger.format(): String {
    return defaultNumberFormatter.format(BigDecimal(this))
}

fun BigDecimal.formatAsChange(): String {
    val prefix = if (isNonNegative) "+" else ""

    return prefix + formatAsPercentage()
}

fun BigDecimal.formatAsPercentage(): String {
    return defaultAbbreviationFormatter.format(this) + "%"
}

fun Percent.format(): String {
    return value.toBigDecimal().formatAsPercentage()
}

fun BigDecimal.formatFractionAsPercentage(): String {
    return fractionToPercentage().formatAsPercentage()
}

fun Date.formatDateSinceEpoch(resourceManager: ResourceManager): String {
    val currentDays = System.currentTimeMillis().daysFromMillis()
    val diff = currentDays - time.daysFromMillis()

    if (diff < 0) throw IllegalArgumentException("Past date should be less than current")
    return when (diff) {
        0L -> resourceManager.getString(R.string.today)
        1L -> resourceManager.getString(R.string.yesterday)
        else -> {
            resourceManager.formatDate(time)
        }
    }
}

fun Long.formatDaysSinceEpoch(context: Context): String? {
    val currentDays = System.currentTimeMillis().daysFromMillis()
    val diff = currentDays - this

    if (diff < 0) throw IllegalArgumentException("Past date should be less than current")

    return when (diff) {
        0L -> context.getString(R.string.today)
        1L -> context.getString(R.string.yesterday)
        else -> {
            val inMillis = TimeUnit.DAYS.toMillis(this)
            DateUtils.formatDateTime(context, inMillis, 0)
        }
    }
}

fun Long.formatDateTime() = dateTimeFormat.format(Date(this))

fun parseDateISO_8601(value: String): Date? {
    return runCatching { dateTimeFormatISO_8601.parse(value) }.getOrNull()
}

fun parseDateISO_8601_NoMs(value: String): Date? {
    return runCatching { dateTimeFormatISO_8601_NoMs.parse(value) }.getOrNull()
}

fun decimalFormatterFor(pattern: String, roundingMode: RoundingMode): DecimalFormat {
    return DecimalFormat(pattern).apply {
        val symbols = decimalFormatSymbols

        symbols.groupingSeparator = GROUPING_SEPARATOR
        symbols.decimalSeparator = DECIMAL_SEPARATOR

        decimalFormatSymbols = symbols

        this.roundingMode = roundingMode
        decimalFormatSymbols = decimalFormatSymbols
    }
}

fun patternWith(precision: Int) = "$DECIMAL_PATTERN_BASE${"#".repeat(precision)}"

fun defaultNumberFormatter() = CompoundNumberFormatter(
    abbreviations = listOf(
        NumberAbbreviation(
            threshold = BigDecimal.ZERO,
            divisor = BigDecimal.ONE,
            suffix = "",
            formatter = DynamicPrecisionFormatter(minPrecision = FULL_PRECISION)
        ),
        NumberAbbreviation(
            threshold = BigDecimal.ONE,
            divisor = BigDecimal.ONE,
            suffix = "",
            formatter = defaultFullFormatter
        ),
        thousandAbbreviation,
        millionAbbreviation,
        billionAbbreviation,
        trillionAbbreviation
    )
)

fun currencyFormatter() = CompoundNumberFormatter(
    abbreviations = listOf(
        NumberAbbreviation(
            threshold = BigDecimal.ZERO,
            divisor = BigDecimal.ONE,
            suffix = "",
            formatter = DynamicPrecisionFormatter(minPrecision = ABBREVIATED_PRECISION)
        ),
        NumberAbbreviation(
            threshold = BigDecimal.ONE,
            divisor = BigDecimal.ONE,
            suffix = "",
            formatter = defaultAbbreviationFormatter
        ),
        thousandAbbreviation,
        millionAbbreviation,
        billionAbbreviation,
        trillionAbbreviation
    )
)

fun Duration.format(
    estimated: Boolean,
    context: Context,
    timeFormat: TimeFormatter?
): String = format(
    estimated = estimated,
    daysFormat = { context.resources.getQuantityString(R.plurals.staking_main_lockup_period_value, it, it) },
    hoursFormat = { context.resources.getQuantityString(R.plurals.common_hours_format, it, it) },
    minutesFormat = { context.resources.getQuantityString(R.plurals.common_minutes_format, it, it) },
    timeFormat = timeFormat
)

typealias TimeFormatter = (hours: Int, minutes: Int, seconds: Int) -> String

inline fun Duration.format(
    estimated: Boolean,
    daysFormat: (days: Int) -> String,
    hoursFormat: (hours: Int) -> String,
    minutesFormat: (minutes: Int) -> String,
    noinline timeFormat: TimeFormatter?
): String {
    val withoutPrefix = toComponents { days, hours, minutes, seconds, _ ->
        when {
            // if duration is zero, we want to display "0 days"
            this == Duration.ZERO -> daysFormat(0)
            // format days + hours if both are present
            days > 0 && hours > 0 -> "${daysFormat(days.toInt())} ${hoursFormat(hours)}"
            // only days in case there is no hours
            days > 0 -> daysFormat(days.toInt())
            // if timeFormat is given, format with it in case there is less then 1 day left
            timeFormat != null -> timeFormat(hours, minutes, seconds)
            // format hours if present
            hours > 0 -> hoursFormat(hours)
            // format minutes otherwise
            else -> minutesFormat(minutes)
        }
    }

    return if (estimated) {
        "~$withoutPrefix"
    } else {
        withoutPrefix
    }
}
