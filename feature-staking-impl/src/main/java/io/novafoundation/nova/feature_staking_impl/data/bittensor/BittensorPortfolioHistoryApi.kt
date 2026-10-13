package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioStakeHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioStakePoint
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.math.BigDecimal
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

interface BittensorPortfolioHistoryApi {

    @POST
    suspend fun history(@Url url: String, @Body body: BittensorPortfolioHistoryRequest): Response<BittensorPortfolioHistoryRemote>
}

/** Strict account-search body: exactly the subject and the preset, no page or window selection. */
class BittensorPortfolioHistoryRequest(val accountSubject: String, val period: String)

class BittensorPortfolioHistoryRemote(
    val period: String?,
    val window: Window?,
    val points: List<Point>?,
    val historyScope: String?,
    val meta: Meta?
) {
    class Window(val start: String?, val end: String?)
    class Point(val timestamp: String?, val reportedValueTao: String?, val reportedValueUsd: String?, val completed: Boolean?)
    class Meta(val completeness: String?)
}

internal const val BITTENSOR_PORTFOLIO_HISTORY_MAX_POINTS = 512

private const val HISTORY_SCOPE_TAO_APP_PARTIAL = "TAO_APP_PARTIAL"
private const val COMPLETENESS_COMPLETE = "COMPLETE"

/**
 * Wire checks of the portfolio-history contract: a body that breaks any of them is a failed request,
 * never a drawn curve. Throws [IllegalArgumentException] naming the offending field.
 */
internal fun bindBittensorPortfolioHistory(
    remote: BittensorPortfolioHistoryRemote,
    period: BittensorPortfolioHistoryPeriod
): BittensorPortfolioStakeHistory {
    require(remote.period == period.wireName) { "period" }
    require(remote.historyScope == HISTORY_SCOPE_TAO_APP_PARTIAL) { "historyScope" }
    require(remote.meta?.completeness == COMPLETENESS_COMPLETE) { "meta.completeness" }

    val window = requireNotNull(remote.window) { "window" }
    val start = requireNotNull(parseBittensorInstant(window.start)) { "window.start" }
    val end = requireNotNull(parseBittensorInstant(window.end)) { "window.end" }
    require(start <= end) { "window" }

    val points = remote.points.orEmpty()
    require(points.size <= BITTENSOR_PORTFOLIO_HISTORY_MAX_POINTS) { "points" }

    var previous: Long? = null
    val bound = points.mapIndexed { index, point ->
        val field = "points[$index]"
        val timestamp = requireNotNull(parseBittensorInstant(point.timestamp)) { "$field.timestamp" }
        require(timestamp in start..end && previous.let { it == null || it < timestamp }) { "$field.timestamp" }
        previous = timestamp

        BittensorPortfolioStakePoint(
            timestamp = timestamp,
            taoValue = bittensorReportedDecimal(point.reportedValueTao, "$field.reportedValueTao"),
            usdValue = bittensorReportedDecimal(point.reportedValueUsd, "$field.reportedValueUsd"),
            completed = requireNotNull(point.completed) { "$field.completed" }
        )
    }

    return BittensorPortfolioStakeHistory(period, start, end, bound)
}

private val INSTANT_PATTERN = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d{1,9})?Z""")

/** RFC 3339 UTC instant (`2026-09-02T13:00:00Z`, optional fraction) to epoch seconds, or null when malformed. */
internal fun parseBittensorInstant(text: String?): Long? {
    val match = text?.let(INSTANT_PATTERN::matchEntire) ?: return null
    val (year, month, day, hour, minute, second) = match.destructured.toList().map(String::toInt)

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

private val REPORTED_DECIMAL_PATTERN = Regex("""-?[0-9]+(\.[0-9]+)?""")

private fun bittensorReportedDecimal(text: String?, field: String): BigDecimal {
    val normalized = text?.trim()?.takeIf(REPORTED_DECIMAL_PATTERN::matches)
    return requireNotNull(normalized) { field }.toBigDecimal()
}

private operator fun <T> List<T>.component6(): T = this[5]
