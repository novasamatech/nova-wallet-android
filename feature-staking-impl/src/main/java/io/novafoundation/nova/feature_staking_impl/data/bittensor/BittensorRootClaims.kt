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
    val safeModeActive: Boolean = false
) {

    val hasRewardsBelowMinimum: Boolean
        get() = previews.any { it.accrued.isPositive() } && claimable.isEmpty()

    val claimable: List<BittensorRootClaimPreview>
        get() = previews.filter { it.redeemable.isPositive() && it.redeemable >= minimum }
            .sortedWith(
                compareByDescending<BittensorRootClaimPreview> { it.redeemable }
                    .thenBy { it.hotkey.value.toHexString() }
            )
}
