package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartPeriod

internal fun BittensorSubnetChartPeriod.labelRes(): Int = when (this) {
    BittensorSubnetChartPeriod.DAY -> R.string.price_chart_day
    BittensorSubnetChartPeriod.WEEK -> R.string.bittensor_chart_week
    BittensorSubnetChartPeriod.MONTH -> R.string.bittensor_chart_month
    BittensorSubnetChartPeriod.QUARTER -> R.string.bittensor_chart_quarter
    BittensorSubnetChartPeriod.YEAR -> R.string.price_chart_year
}
