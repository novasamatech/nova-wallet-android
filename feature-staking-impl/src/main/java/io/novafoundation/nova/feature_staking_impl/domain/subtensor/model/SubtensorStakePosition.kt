package io.novafoundation.nova.feature_staking_impl.domain.subtensor.model

import java.math.BigInteger

/**
 * Resolved stake position for a (coldkey, hotkey, netuid) tuple as returned
 * by `StakeInfoRuntimeApi_get_stake_info_for_coldkey`. Amount is already in
 * the subnet's smallest unit (RAO for netuid=0, alpha-units for netuid>0).
 */
data class SubtensorStakePosition(
    val coldkey: ByteArray,
    val hotkey: ByteArray,
    val netuid: Int,
    val amount: BigInteger,
    val validatorIdentity: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubtensorStakePosition) return false
        return coldkey.contentEquals(other.coldkey) &&
            hotkey.contentEquals(other.hotkey) &&
            netuid == other.netuid &&
            amount == other.amount &&
            validatorIdentity == other.validatorIdentity
    }

    override fun hashCode(): Int {
        var result = coldkey.contentHashCode()
        result = 31 * result + hotkey.contentHashCode()
        result = 31 * result + netuid
        result = 31 * result + amount.hashCode()
        result = 31 * result + (validatorIdentity?.hashCode() ?: 0)
        return result
    }
}
