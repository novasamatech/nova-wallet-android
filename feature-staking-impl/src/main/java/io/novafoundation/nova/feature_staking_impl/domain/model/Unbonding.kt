package io.novafoundation.nova.feature_staking_impl.domain.model

import java.math.BigInteger

class Unbonding(val amount: BigInteger, val status: Status) {

    sealed class Status {

        class Unbonding(val timeLeft: Long, val calculatedAt: Long) : Status()

        object Redeemable : Status()
    }
}
