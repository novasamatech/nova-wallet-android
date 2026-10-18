package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import java.math.BigInteger

internal fun bittensorMinimumRootClaim(bits: BigInteger?): BigInteger {
    if (bits == null) return BittensorProtocolUnits.DEFAULT_ROOT_CLAIM_THRESHOLD_RAO
    require(bits.isNonNegative && bits.bitLength() <= 128)

    val unit = BigInteger.ONE.shiftLeft(BittensorProtocolUnits.FIXED_POINT_FRACTION_BITS)
    return (bits + unit - BigInteger.ONE)
        .shiftRight(BittensorProtocolUnits.FIXED_POINT_FRACTION_BITS)
}

internal fun bindBittensorRootClaimPreviews(decoded: Any?): List<BittensorRootClaimPreview> {
    val entries = decoded as? List<*> ?: error("Invalid root claim previews")
    return entries.map { value ->
        val row = value.castToStruct()
        val hotkey = bindAccountIdKey(row["hotkey"])
        require(hotkey.value.size == 32)
        val accrued = bindNumber(row["accruedTao"])
        val redeemable = bindNumber(row["redeemableTao"])
        val forfeited = bindNumber(row["forfeitedTaoEst"])
        require(
            listOf(accrued, redeemable, forfeited).all { it.isNonNegative && it.bitLength() <= 128 }
        )
        BittensorRootClaimPreview(
            hotkey,
            accrued,
            redeemable,
            forfeited
        )
    }
        .also { rows ->
            require(
                rows.map { it.hotkey }
                    .distinct().size == rows.size
            ) { "Duplicate root claim hotkeys" }
        }
}
