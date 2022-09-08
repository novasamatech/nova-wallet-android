package io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository

import java.math.BigInteger

class OptimalAutomationRequest(
    val collator: String,
    val amount: BigInteger,
)

data class OptimalAutomationResponse(
    val apy: Double,
    val period: Int,
)
