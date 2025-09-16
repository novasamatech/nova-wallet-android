package io.novafoundation.nova.common.utils.formatting

import android.content.Context
import android.text.format.DateUtils
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.daysFromMillis
import io.novafoundation.nova.common.utils.formatting.duration.BoundedDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.CompoundDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DayAndHourDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DayDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.HoursDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.MinutesDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.RoundMinutesDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.SecondsDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.ZeroDurationFormatter
import io.novafoundation.nova.common.utils.fractionToPercentage
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.toPercent
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

const val DATE_ISO_8601_FULL = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
const val DATE_ISO_8601_NO_MS = "yyyy-MM-dd'T'HH:mm:ss'Z'"

private const val DECIMAL_PATTERN_BASE = "###,###."

private const val GROUPING_SEPARATOR = ','
private const val DECIMAL_SEPARATOR = '.'

private const val MIN_SCALE = 5
private const val PRICE_MIN_PRECISION = 3
private const val TOKEN_MIN_PRECISION = 1
const val ABBREVIATED_SCALE = 2

private val dateTimeFormat = SimpleDateFormat.getDateTimeInstance()
private val dateTimeFormatISO_8601 by lazy { SimpleDateFormat(DATE_ISO_8601_FULL, Locale.getDefault()) }
private val dateTimeFormatISO_8601_NoMs by lazy { SimpleDateFormat(DATE_ISO_8601_NO_MS, Locale.getDefault()) }

private val defaultAbbreviationFormatter = FixedPrecisionFormatter(ABBREVIATED_SCALE)
private val defaultFullFormatter = FixedPrecisionFormatter(MIN_SCALE)

private val zeroAbbreviation = NumberAbbreviation(
    threshold = BigDecimal.ZERO,
    divisor = BigDecimal.ONE,
    suffix = "",
    formatter = DynamicPrecisionFormatter(minScale = MIN_SCALE, minPrecision = TOKEN_MIN_PRECISION)
)

private val oneAbbreviation = NumberAbbreviation(
    threshold = BigDecimal.ONE,
    divisor = BigDecimal.ONE,
    suffix = "",
    formatter = defaultFullFormatter
)

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
private val fullAmountAbbreviationFormatter = fullAmountAbbreviationFormatter()

fun BigDecimal.toStripTrailingZerosString(): String {
    return stripTrailingZeros().toPlainString()
}

fun BigDecimal.format(roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return defaultNumberFormatter.format(this, roundingMode)
}

@Deprecated("Use TokenFormatter instead")
fun BigDecimal.formatWithFullAmount(): String {
    return fullAmountAbbreviationFormatter.format(this)
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

fun BigDecimal.formatAsPercentage(includeSymbol: Boolean = true): String {
    return defaultAbbreviationFormatter.format(this) + if (includeSymbol) "%" else ""
}

fun Percent.format(): String {
    return value.toBigDecimal().formatAsPercentage()
}

fun Fraction.formatPercents(): String {
    return inPercents.toBigDecimal().formatAsPercentage()
}

fun Perbill.format(): String {
    return toPercent().format()
}

fun Fraction.formatPercents(includeSymbol: Boolean = true): String {
    return inPercents.toBigDecimal().formatAsPercentage(includeSymbol)
}

fun BigDecimal.formatFractionAsPercentage(): String {
    return fractionToPercentage().formatAsPercentage()
}

fun Date.formatDateSinceEpoch(resourceManager: ResourceManager): String {
    val currentDays = System.currentTimeMillis().daysFromMillis()
    val diff = currentDays - time.daysFromMillis()

    return when (diff) {
        0L -> resourceManager.getString(R.string.today)
        1L -> resourceManager.getString(R.string.yesterday)
        else -> {
            resourceManager.formatDate(time)
        }
    }
}

fun Date.isThisYear(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time
    return calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
}

fun ResourceManager.formatTime(data: Date): String {
    return formatTime(data.time)
}

fun Long.formatDaysSinceEpoch(context: Context): String? {
    val currentDays = System.currentTimeMillis().daysFromMillis()
    val diff = currentDays - this

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

fun formatDateISO_8601_NoMs(date: Date): String {
    return dateTimeFormatISO_8601_NoMs.format(date)
}

fun decimalFormatterFor(pattern: String): DecimalFormat {
    return DecimalFormat(pattern).apply {
        val symbols = decimalFormatSymbols

        symbols.groupingSeparator = GROUPING_SEPARATOR
        symbols.decimalSeparator = DECIMAL_SEPARATOR

        decimalFormatSymbols = symbols

        decimalFormatSymbols = decimalFormatSymbols
    }
}

fun CharSequence.toAmountWithFraction(): AmountWithFraction {
    val amountAndFraction = this.split(DECIMAL_SEPARATOR)
    val amount = amountAndFraction[0]
    val fraction = amountAndFraction.getOrNull(1)
    return AmountWithFraction(amount, fraction, DECIMAL_SEPARATOR.toString())
}

fun patternWith(precision: Int) = "$DECIMAL_PATTERN_BASE${"#".repeat(precision)}"

fun fullAmountAbbreviationFormatter() = CompoundNumberFormatter(
    abbreviations = listOf(
        zeroAbbreviation,
        oneAbbreviation,
        thousandAbbreviation
    )
)

fun defaultNumberFormatter() = CompoundNumberFormatter(
    abbreviations = listOf(
        zeroAbbreviation,
        oneAbbreviation,
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
            formatter = DynamicPrecisionFormatter(minScale = ABBREVIATED_SCALE, minPrecision = PRICE_MIN_PRECISION)
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

fun simpleCurrencyFormatter() = CompoundNumberFormatter(
    abbreviations = listOf(
        NumberAbbreviation(
            threshold = BigDecimal.ZERO,
            divisor = BigDecimal.ONE,
            suffix = "",
            formatter = DynamicPrecisionFormatter(minScale = ABBREVIATED_SCALE, minPrecision = PRICE_MIN_PRECISION)
        ),
        NumberAbbreviation(
            threshold = BigDecimal.ONE,
            divisor = BigDecimal.ONE,
            suffix = "",
            formatter = defaultAbbreviationFormatter
        )
    )
)

fun baseDurationFormatter(
    context: Context,
    dayDurationFormatter: BoundedDurationFormatter = DayAndHourDurationFormatter(
        DayDurationFormatter(context),
        HoursDurationFormatter(context)
    ),
    hoursDurationFormatter: BoundedDurationFormatter = HoursDurationFormatter(context),
    minutesDurationFormatter: BoundedDurationFormatter = MinutesDurationFormatter(context),
    secondsDurationFormatter: BoundedDurationFormatter = SecondsDurationFormatter(context),
    zeroDurationFormatter: BoundedDurationFormatter = ZeroDurationFormatter(DayDurationFormatter(context))
): DurationFormatter {
    val compoundFormatter = CompoundDurationFormatter(
        dayDurationFormatter,
        hoursDurationFormatter,
        minutesDurationFormatter,
        secondsDurationFormatter,
        zeroDurationFormatter
    )

    return RoundMinutesDurationFormatter(compoundFormatter)
}
