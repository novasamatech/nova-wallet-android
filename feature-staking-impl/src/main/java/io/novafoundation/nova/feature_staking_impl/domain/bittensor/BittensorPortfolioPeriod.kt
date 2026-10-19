package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod

enum class BittensorPortfolioPeriod(
    val sampleStepSeconds: Long,
    val fxToleranceSeconds: Long,
    val priceSource: PricePeriod
) {
    ONE_DAY(3600, 600, PricePeriod.DAY),
    SEVEN_DAYS(3600, 7200, PricePeriod.MONTH),
    THIRTY_DAYS(14400, 7200, PricePeriod.MONTH),
    NINETY_DAYS(28800, 172800, PricePeriod.MAX)
}
