package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import java.math.BigInteger

class CandidateMetadata(
    val delegationCount: BigInteger
)

fun CandidateMetadata.isFull(maxAllowedDelegators: BigInteger): Boolean {
    return delegationCount == maxAllowedDelegators
}

fun bindCandidateMetadata(decoded: Any?): CandidateMetadata {
    return decoded.castToStruct().let { struct ->
        CandidateMetadata(
            delegationCount = bindNumber(struct["delegationCount"])
        )
    }
}
