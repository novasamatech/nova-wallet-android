package io.novafoundation.nova.feature_staking_impl.domain.model

import io.novafoundation.nova.common.utils.formatting.TimerValue
import java.math.BigInteger

class Unbonding(val id: String, val amount: BigInteger, val status: Status) {

    sealed class Status {

        data class Unbonding(val timer: TimerValue) : Status()

        object Redeemable : Status()
    }
}

val Unbonding.isRedeemable: Boolean
    get() = status is Unbonding.Status.Redeemable
