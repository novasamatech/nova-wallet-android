package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.data.repository.duration
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal fun PricePeriod.historySource(): PricePeriod = when (this) {
    PricePeriod.WEEK -> PricePeriod.MONTH
    PricePeriod.YEAR -> PricePeriod.MAX
    else -> this
}

internal fun PricePeriod.samplingInterval(): Duration = when (this) {
    PricePeriod.DAY -> 5.minutes
    PricePeriod.WEEK, PricePeriod.MONTH -> 1.hours
    PricePeriod.YEAR, PricePeriod.MAX -> 1.days
}

internal fun nearestBittensorPriceIndex(timestamps: List<Long>, timestamp: Long, tolerance: Long): Int? {
    val search = timestamps.binarySearch(timestamp)
    if (search >= 0) return search
    val insertion = -search - 1
    return listOf(insertion - 1, insertion)
        .filter { it in timestamps.indices }
        .minByOrNull { abs(timestamps[it] - timestamp) }
        ?.takeIf { abs(timestamps[it] - timestamp) <= tolerance }
}

internal fun hasBittensorPeriodCoverage(points: List<HistoricalCoinRate>, period: PricePeriod): Boolean {
    if (points.size < 2) return false
    if (period == PricePeriod.MAX) return true
    val coveredSeconds = points.last().timestamp - points.first().timestamp
    return coveredSeconds + 2 * period.samplingInterval().inWholeSeconds >= period.duration().inWholeSeconds
}
