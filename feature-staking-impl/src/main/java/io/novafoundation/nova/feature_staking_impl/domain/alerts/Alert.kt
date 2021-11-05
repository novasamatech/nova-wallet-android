package io.novafoundation.nova.feature_staking_impl.domain.alerts

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal

sealed class Alert {

    class RedeemTokens(val amount: BigDecimal, val token: Token) : Alert()

    class BondMoreTokens(val minimalStake: BigDecimal, val token: Token) : Alert()

    object ChangeValidators : Alert()

    object WaitingForNextEra : Alert()

    object SetValidators : Alert()
}
