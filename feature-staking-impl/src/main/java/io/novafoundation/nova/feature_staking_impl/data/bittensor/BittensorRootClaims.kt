package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

data class BittensorRootClaimPreview(
    val hotkey: AccountIdKey,
    val accrued: BigInteger,
    val redeemable: BigInteger,
    val forfeitedEstimate: BigInteger
)

data class BittensorRootClaims(
    val previews: List<BittensorRootClaimPreview>,
    val minimum: BigInteger,
    val coldkeySwapPending: Boolean = false,
    val safeModeActive: Boolean = false,
    val rootStake: BigInteger? = null,
    val rootHoldDurationMillis: BigInteger? = null,
    val readStartedAtMillis: Long? = null
) {

    val hasRewardsBelowMinimum: Boolean
        get() = previews.any { it.redeemable.isPositive() } && claimable.isEmpty()

    val totalRedeemable: BigInteger
        get() = previews.fold(BigInteger.ZERO) { total, preview -> total + preview.redeemable }

    /** Rewards that meet the claim minimum, like iOS SubtensorState.totalRootRedeemable. */
    val totalClaimable: BigInteger
        get() = claimable.fold(BigInteger.ZERO) { total, preview -> total + preview.redeemable }

    val claimable: List<BittensorRootClaimPreview>
        get() = previews.filter { it.redeemable.isPositive() && it.redeemable >= minimum }
            .sortedWith(
                compareByDescending<BittensorRootClaimPreview> { it.redeemable }
                    .thenBy { it.hotkey.value.toHexString() }
            )
}
