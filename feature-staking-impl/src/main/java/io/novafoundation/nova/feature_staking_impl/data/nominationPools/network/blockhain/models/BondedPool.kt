package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindCollectionEnum
import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbillTyped
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.Perbill

class BondedPool(
    val poolId: PoolId,
    val points: PoolPoints,
    val commission: PoolCommission?,
    val state: PoolState
)

class PoolCommission(val current: Current?) {

    class Current(val perbill: Perbill)
}

enum class PoolState {

    Open, Blocked, Destroying
}

fun bindBondedPool(decoded: Any, poolId: PoolId): BondedPool {
    val asStruct = decoded.castToStruct()

    return BondedPool(
        points = bindPoolPoints(asStruct["points"]),
        poolId = poolId,
        commission = asStruct.get<Any?>("commission")?.let(::bindPoolCommission),
        state = bindCollectionEnum(asStruct["state"])
    )
}

fun bindPoolCommission(decoded: Any): PoolCommission {
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
