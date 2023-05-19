package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.BigDecimal

enum class RewardSuffix(@StringRes val suffixResourceId: Int?) {
    None(null),
    APY(R.string.staking_apy),
    APR(R.string.staking_apr)
}

fun RewardSuffix.format(resourceManager: ResourceManager, gainsFraction: BigDecimal): String {
    val gainsFormatted = gainsFraction.formatFractionAsPercentage()

    return suffixResourceId?.let { resourceManager.getString(it, gainsFormatted) } ?: gainsFormatted
}

fun mapPeriodReturnsToRewardEstimation(
    periodReturns: PeriodReturns,
    token: Token,
    resourceManager: ResourceManager,
    rewardSuffix: RewardSuffix = RewardSuffix.None,
): RewardEstimation {
    val gainWithSuffix = rewardSuffix.format(resourceManager, periodReturns.gainFraction)

    val amountFormatted = periodReturns.gainAmount.formatTokenAmount(token.configuration)
    val amountWithSuffix = resourceManager.getString(R.string.common_per_year_format, amountFormatted)

    return RewardEstimation(
        amount = amountWithSuffix,
        fiatAmount = token.amountToFiat(periodReturns.gainAmount).formatAsCurrency(token.currency),
        gain = gainWithSuffix
    )
}
