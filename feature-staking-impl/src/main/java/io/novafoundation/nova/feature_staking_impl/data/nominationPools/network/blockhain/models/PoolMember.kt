package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class PoolMember(
    val accountId: AccountId,
    val poolId: PoolId,
    val points: PoolPoints,
)

fun bindPoolMember(decoded: Any, accountId: AccountId) : PoolMember {
    val asStruct = decoded.castToStruct()

    return PoolMember(
        accountId = accountId,
        poolId = bindPoolId(asStruct["poolId"]),
        points = bindPoolPoints(asStruct["points"])
    )
}
