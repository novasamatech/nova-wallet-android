package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts

import java.math.BigInteger

sealed class ParachainStakingAlert {

    object ChangeCollator : ParachainStakingAlert()

    object StakeMore : ParachainStakingAlert()

    class RedeemTokens(val redeemableAmount: BigInteger) : ParachainStakingAlert()
}
