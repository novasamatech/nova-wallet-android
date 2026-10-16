package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger

@JvmInline
value class BittensorSubnetId(val value: Int) {

    init {
        require(value in 1..65535) { "Dynamic subnet id must fit into a non-zero u16" }
    }
}

data class BittensorSubnetPosition(
    val subnetId: BittensorSubnetId,
    val validatorHotkey: AccountIdKey,
    val alphaInPlanks: BigInteger,
    val isRegistered: Boolean? = null
)

enum class BittensorSwapDirection {
    TAO_TO_ALPHA, ALPHA_TO_TAO
}

/** Runtime simulation only: Nova service commission is not included in these amounts. */
data class BittensorSwapSimulation(
    val subnetId: BittensorSubnetId,
    val direction: BittensorSwapDirection,
    /** Net pool input, excluding the runtime fee in the input token. */
    val amountIn: BigInteger,
    val amountOut: BigInteger,
    val taoFee: BigInteger,
    val alphaFee: BigInteger,
    val taoSlippage: BigInteger,
    val alphaSlippage: BigInteger
)
