package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import java.math.BigInteger
import java.math.RoundingMode

/**
 * A minimum the user has to enter, rounded up like iOS `formatRequiredAmount(..., roundingMode: .up)`:
 * typing the displayed value must pass the check that produced it.
 */
fun TokenFormatter.formatBittensorRequiredAmount(planks: BigInteger, token: Token): CharSequence = formatToken(
    token.amountFromPlanks(planks),
    token.configuration.symbol,
    TokenConfig(roundingMode = RoundingMode.CEILING)
)
