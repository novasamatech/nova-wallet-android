package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.formatFractionAsPercentage
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount

enum class RewardSuffix(@StringRes val suffixResourceId: Int?) {
    None(null),
    APY(R.string.staking_apy),
    APR(R.string.staking_apr)
}

fun mapPeriodReturnsToRewardEstimation(
    periodReturns: PeriodReturns,
    token: Token,
    resourceManager: ResourceManager,
    rewardSuffix: RewardSuffix = RewardSuffix.None,
): RewardEstimation {

    val gainFormatted = periodReturns.gainFraction.formatFractionAsPercentage()
    val gainWithSuffix = rewardSuffix.suffixResourceId?.let { resourceManager.getString(it, gainFormatted) } ?: gainFormatted

    return RewardEstimation(
        amount = periodReturns.gainAmount.formatTokenAmount(token.configuration),
        fiatAmount = token.fiatAmount(periodReturns.gainAmount).formatAsCurrency(),
        gain = gainWithSuffix
    )
}
