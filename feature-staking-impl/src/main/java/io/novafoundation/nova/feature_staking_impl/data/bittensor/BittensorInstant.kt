package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.util.GregorianCalendar
import java.util.TimeZone

private val INSTANT_PATTERN = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d{1,9})?Z""")

/**
 * RFC 3339 UTC instant of the Bittensor service contracts (`2026-09-02T13:00:00Z`, optional fraction) to epoch
 * seconds, or null when malformed. Offsets other than `Z` are rejected: the service promises UTC.
 * Hand-rolled because minSdk 24 has no `java.time` and the project does not desugar it.
 */
internal fun parseBittensorInstant(text: String?): Long? {
    val groups = text?.let(INSTANT_PATTERN::matchEntire)?.groupValues ?: return null
    val (year, month, day) = groups.subList(1, 4).map(String::toInt)
    val (hour, minute, second) = groups.subList(4, 7).map(String::toInt)

    val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        isLenient = false
        clear()
        set(year, month - 1, day, hour, minute, second)
    }

    return try {
        calendar.timeInMillis / 1000
    } catch (e: IllegalArgumentException) {
        null
    }
}
