package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.alerts

import java.math.BigInteger

sealed class MythosStakingAlert {

    object ChangeCollator : MythosStakingAlert()

    class RedeemTokens(val redeemableAmount: BigInteger) : MythosStakingAlert()
}
