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

enum class RewardSuffix(@StringRes val suffixResourceId: Int) {
    APY(R.string.staking_apy),
    APR(R.string.staking_apr)
}

fun RewardSuffix.format(resourceManager: ResourceManager, gainsFraction: BigDecimal): String {
    val gainsFormatted = gainsFraction.formatFractionAsPercentage()

    return resourceManager.getString(suffixResourceId, gainsFormatted)
}

fun PeriodReturns.rewardSuffix(): RewardSuffix {
    return if (isCompound) RewardSuffix.APY else RewardSuffix.APR
}

fun mapPeriodReturnsToRewardEstimation(
    periodReturns: PeriodReturns,
    token: Token,
    resourceManager: ResourceManager,
): RewardEstimation {
    val suffix = periodReturns.rewardSuffix()

    val gainWithSuffix = suffix.format(resourceManager, periodReturns.gainFraction)

    val amountFormatted = periodReturns.gainAmount.formatTokenAmount(token.configuration)
    val amountWithSuffix = resourceManager.getString(R.string.common_per_year_format, amountFormatted)

    return RewardEstimation(
        amount = amountWithSuffix,
        fiatAmount = token.amountToFiat(periodReturns.gainAmount).formatAsCurrency(token.currency),
        gain = gainWithSuffix
    )
}
