package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class CandidateMetadata(
    val totalCounted: Balance,
    val delegationCount: BigInteger,
    val lowestBottomDelegationAmount: Balance,
)

fun CandidateMetadata.isFull(maxAllowedDelegators: BigInteger): Boolean {
    return delegationCount == maxAllowedDelegators
}
fun CandidateMetadata.isRewardedListFull(maxRewardedCollators: BigInteger): Boolean {
    return totalCounted >= maxRewardedCollators
}

fun bindCandidateMetadata(decoded: Any?): CandidateMetadata {
    return decoded.castToStruct().let { struct ->
        CandidateMetadata(
            totalCounted = bindNumber(struct["totalCounted"]),
            delegationCount = bindNumber(struct["delegationCount"]),
            lowestBottomDelegationAmount = bindNumber(struct["lowestBottomDelegationAmount"])
        )
    }
}
