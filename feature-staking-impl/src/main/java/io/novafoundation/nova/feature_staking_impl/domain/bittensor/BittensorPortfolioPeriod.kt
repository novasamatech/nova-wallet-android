package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class BittensorPortfolioPeriod(
    val sampleStepSeconds: Long,
    val fxToleranceSeconds: Long,
    val priceSource: PricePeriod
) {
    ONE_DAY(
        sampleStepSeconds = 1.hours.inWholeSeconds,
        fxToleranceSeconds = 10.minutes.inWholeSeconds,
        priceSource = PricePeriod.DAY
    ),
    SEVEN_DAYS(
        sampleStepSeconds = 1.hours.inWholeSeconds,
        fxToleranceSeconds = 2.hours.inWholeSeconds,
        priceSource = PricePeriod.MONTH
    ),
    THIRTY_DAYS(
        sampleStepSeconds = 4.hours.inWholeSeconds,
        fxToleranceSeconds = 2.hours.inWholeSeconds,
        priceSource = PricePeriod.MONTH
    ),
    NINETY_DAYS(
        sampleStepSeconds = 8.hours.inWholeSeconds,
        fxToleranceSeconds = 2.days.inWholeSeconds,
        priceSource = PricePeriod.MAX
    )
}
