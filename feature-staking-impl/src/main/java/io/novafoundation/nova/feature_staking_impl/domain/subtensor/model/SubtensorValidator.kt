package io.novafoundation.nova.feature_staking_impl.domain.subtensor.model

import java.math.BigInteger

/**
 * Validator (hotkey) display row for the Bittensor validator picker.
 *
 * Identity comes from the `opentensor/bittensor-delegates` GitHub registry;
 * numeric fields (totalStake, ownStake, commission, apr, nominatorCount)
 * come from a TaoStats stop-gap until Nova's own indexer ships.
 *
 * Mirrors iOS `SubtensorValidator`.
 */
data class SubtensorValidator(
    val hotkey: ByteArray,
    val netuid: Int,
    val identity: String?,
    val url: String?,
    val description: String?,
    val totalStake: BigInteger,
    val ownStake: BigInteger,
    val nominatorCount: Int?,
    /** Fraction 0.0..1.0. Null when unknown. */
    val commission: Double?,
    /** Post-commission APR. Null when unknown. */
    val apr: Double?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubtensorValidator) return false
        return hotkey.contentEquals(other.hotkey) && netuid == other.netuid
    }

    override fun hashCode(): Int {
        var result = hotkey.contentHashCode()
        result = 31 * result + netuid
        return result
    }
}
