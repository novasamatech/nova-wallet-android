package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import java.math.BigInteger

@JvmInline
value class PoolPoints(val value: BigInteger)

@Suppress("NOTHING_TO_INLINE")
inline fun bindPoolPoints(decoded: Any?) = PoolPoints(bindNumber(decoded))
