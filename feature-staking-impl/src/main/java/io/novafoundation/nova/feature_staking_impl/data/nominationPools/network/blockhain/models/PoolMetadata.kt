package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindString

@JvmInline
value class PoolMetadata(val title: String)

fun bindPoolMetadata(decoded: Any): PoolMetadata {
    return PoolMetadata(bindString(decoded))
}
