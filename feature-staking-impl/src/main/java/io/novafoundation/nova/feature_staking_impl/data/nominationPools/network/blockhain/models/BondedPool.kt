package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbillTyped
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.Perbill

class BondedPool(
    val poolId: PoolId,
    val points: PoolPoints,
    val commission: PoolCommission,
)

class PoolCommission(val current: Current?) {

        class Current(val perbill: Perbill)
}

fun bindBondedPool(decoded: Any, poolId: PoolId): BondedPool {
    val asStruct = decoded.castToStruct()

    return BondedPool(
        points = bindPoolPoints(asStruct["points"]),
        poolId = poolId,
        commission = bindPoolCommission(asStruct["commission"])
    )
}

fun bindPoolCommission(decoded: Any?): PoolCommission {
    val asStruct = decoded.castToStruct()

    return PoolCommission(
        current = bindCurrentCommission(asStruct["current"])
    )
}

private fun bindCurrentCommission(decoded: Any?): PoolCommission.Current? {
    return decoded?.let {
        val (perbill, _) = decoded.castToList()

        PoolCommission.Current(bindPerbillTyped(perbill))
    }
}
