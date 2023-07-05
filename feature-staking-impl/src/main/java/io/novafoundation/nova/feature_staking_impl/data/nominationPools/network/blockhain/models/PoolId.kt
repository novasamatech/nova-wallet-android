package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import java.math.BigInteger

@JvmInline
value class PoolId(val value: PoolIdRaw)

typealias PoolIdRaw = BigInteger

fun bindPoolId(decoded: Any?): PoolId {
    return PoolId(bindNumber(decoded))
}
