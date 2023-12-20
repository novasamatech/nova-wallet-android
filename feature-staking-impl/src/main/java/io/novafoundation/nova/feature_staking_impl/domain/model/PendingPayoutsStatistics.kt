package io.novafoundation.nova.feature_staking_impl.domain.model

import java.math.BigInteger

class PendingPayoutsStatistics(
    val payouts: List<PendingPayout>,
    val totalAmountInPlanks: BigInteger,
)

data class PendingPayout(
    val validatorInfo: ValidatorInfo,
    val era: BigInteger,
    val amountInPlanks: BigInteger,
    val timeLeft: Long,
    val timeLeftCalculatedAt: Long,
    val closeToExpire: Boolean,
    val pagesToClaim: List<Int>,
) {
    class ValidatorInfo(
        val address: String,
        val identityName: String?,
    )
}
