package io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class GrowthPeriodInfo(
    val index: BigInteger,
)

class GrowthInfo(
    val numberOfAccumulations: BigInteger,
    val totalStakeAccumulated: Balance,
    val totalStakerReward: Balance,
)

fun bindGrowthPeriod(dynamic: Any?): GrowthPeriodInfo {
    val struct = dynamic.castToStruct()
    val index: Any? = struct.mapping["index"]
    return GrowthPeriodInfo(index = bindNumber(index))
}

fun bindGrowthInfo(dynamic: Any?): GrowthInfo {
    val struct = dynamic.castToStruct()

    return GrowthInfo(
        numberOfAccumulations = bindNumber(struct["numberOfAccumulations"]),
        totalStakeAccumulated = bindNumber(struct["totalStakeAccumulated"]),
        totalStakerReward = bindNumber(struct["totalStakerReward"])
    )
}

class CommissionSetting(
    val current: BigInteger,
    val scheduled: BigInteger?,
)

fun bindCommissionSetting(dynamic: Any?): CommissionSetting {
    val struct = dynamic.castToStruct()
    val scheduledRaw: Any? = struct.mapping["scheduled"]

    return CommissionSetting(
        current = bindNumber(struct["current"]),
        scheduled = scheduledRaw?.let(::bindNumber)
    )
}
