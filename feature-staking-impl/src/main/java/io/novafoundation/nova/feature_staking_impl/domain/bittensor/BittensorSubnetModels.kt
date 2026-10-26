package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger

data class BittensorSubnetPosition(
    val subnetId: BittensorNetuid.Subnet,
    val validatorHotkey: AccountIdKey,
    val alphaInPlanks: BigInteger,
    val isRegistered: Boolean? = null
)

enum class BittensorSwapDirection {
    TAO_TO_ALPHA, ALPHA_TO_TAO
}

/** Runtime simulation only: Nova service commission is not included in these amounts. */
data class BittensorSwapSimulation(
    val subnetId: BittensorNetuid.Subnet,
    val direction: BittensorSwapDirection,
    /** Net pool input, excluding the runtime fee in the input token. */

    val amountIn: BigInteger,
    val amountOut: BigInteger,
    val taoFee: BigInteger,
    val alphaFee: BigInteger,
    val taoSlippage: BigInteger? = null,
    val alphaSlippage: BigInteger? = null
)
