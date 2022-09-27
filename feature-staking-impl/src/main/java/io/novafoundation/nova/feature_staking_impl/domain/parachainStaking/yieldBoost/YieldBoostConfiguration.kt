package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost

import java.math.BigInteger

sealed class YieldBoostConfiguration(open val collatorIdHex: String) {

    data class Off(override val collatorIdHex: String) : YieldBoostConfiguration(collatorIdHex)

    data class On(
        val threshold: BigInteger,
        val frequencyInDays: Int,
        override val collatorIdHex: String
    ) : YieldBoostConfiguration(collatorIdHex)
}
