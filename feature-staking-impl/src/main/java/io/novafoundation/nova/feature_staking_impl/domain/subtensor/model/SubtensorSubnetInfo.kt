package io.novafoundation.nova.feature_staking_impl.domain.subtensor.model

import java.math.BigInteger

/**
 * Lightweight model for a Bittensor subnet, used by the subnet picker.
 * Mirrors iOS `SubtensorSubnetInfo`.
 */
data class SubtensorSubnetInfo(
    val netuid: Int,
    val name: String?,
    val taoReserve: BigInteger,
    val alphaInReserve: BigInteger,
) {

    /** Spot price in TAO per alpha: taoReserve / alphaInReserve. */
    val spotPrice: Double
        get() = if (alphaInReserve.signum() > 0) {
            taoReserve.toDouble() / alphaInReserve.toDouble()
        } else {
            0.0
        }
}
