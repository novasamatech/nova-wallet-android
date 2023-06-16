package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct

class BondedPool(
    val poolId: PoolId,
    val points: PoolPoints,
)

fun bindBondedPool(decoded: Any, poolId: PoolId) : BondedPool {
    val asStruct = decoded.castToStruct()

    return BondedPool(
        points = bindPoolPoints(asStruct["points"]),
        poolId = poolId
    )
}
