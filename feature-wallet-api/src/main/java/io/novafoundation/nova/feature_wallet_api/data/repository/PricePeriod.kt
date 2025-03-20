package io.novafoundation.nova.feature_wallet_api.data.repository

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

enum class PricePeriod {
    DAY, WEEK, MONTH, YEAR, MAX
}

fun PricePeriod.duration(): Duration {
    return when (this) {
        PricePeriod.DAY -> 1.days
        PricePeriod.WEEK -> 7.days
        PricePeriod.MONTH -> 30.days
        PricePeriod.YEAR -> 365.days
        PricePeriod.MAX -> Duration.INFINITE
    }
}
