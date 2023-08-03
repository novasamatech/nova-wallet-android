package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindMap
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.UnlockChunk
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindEraIndex
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolBalanceConvertable
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class UnbondingPools(
    val noEra: UnbondingPool,
    val withEra: Map<EraIndex, UnbondingPool>
)

class UnbondingPool(
    override val poolPoints: PoolPoints,
    override val poolBalance: Balance
) : PoolBalanceConvertable

fun UnbondingPools.getPool(era: EraIndex): UnbondingPool {
    return withEra[era] ?: noEra
}

fun UnbondingPools?.unlockChunksFor(poolMember: PoolMember): List<UnlockChunk> {
    if (this == null) return emptyList()

    return poolMember.unbondingEras.map { (unbondEra, unbondPoints) ->
        val unbondingPool = getPool(unbondEra)
        val unbondBalance = unbondingPool.amountOf(unbondPoints)

        UnlockChunk(amount = unbondBalance, era = unbondEra)
    }
}

fun bindUnbondingPools(decoded: Any): UnbondingPools {
    val asStruct = decoded.castToStruct()

    return UnbondingPools(
        noEra = bindUnbondingPool(asStruct["noEra"]),
        withEra = bindMap(
            dynamicInstance = asStruct["withEra"],
            keyBinder = ::bindEraIndex,
            valueBinder = ::bindUnbondingPool
        )
    )
}

fun bindUnbondingPool(decoded: Any?): UnbondingPool {
    val asStruct = decoded.castToStruct()

    return UnbondingPool(
        poolPoints = bindPoolPoints(asStruct["points"]),
        poolBalance = bindNumber(asStruct["balance"])
    )
}
