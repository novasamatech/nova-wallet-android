package io.novafoundation.nova.feature_staking_api.domain.nominationPool.model

import java.math.BigInteger

@JvmInline
value class PoolId(val value: PoolIdRaw)

typealias PoolIdRaw = BigInteger

fun PoolId(id: Int) = PoolId(id.toBigInteger())
