package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import java.math.BigInteger

@JvmInline
value class PoolPoints(val value: BigInteger): Comparable<PoolPoints> {

    operator fun plus(other: PoolPoints): PoolPoints = PoolPoints(value + other.value)

    override fun compareTo(other: PoolPoints): Int {
        return value.compareTo(other.value)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun bindPoolPoints(decoded: Any?) = PoolPoints(bindNumber(decoded))
