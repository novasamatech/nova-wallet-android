package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import java.math.BigDecimal
import java.math.BigInteger

class RewardCalculationTarget(
    val accountIdHex: String,
    val totalStake: BigInteger,
    val ownStake: BigInteger,
    val nominatorStakes: List<IndividualExposure>,
    val commission: BigDecimal
)
