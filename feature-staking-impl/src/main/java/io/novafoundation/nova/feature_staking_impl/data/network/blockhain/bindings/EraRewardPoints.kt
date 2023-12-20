package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.requireType
import io.novafoundation.nova.common.utils.second
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

typealias RewardPoints = BigInteger

class EraRewardPoints(
    val totalPoints: RewardPoints,
    val individual: List<Individual>
) {
    class Individual(val accountId: AccountId, val rewardPoints: RewardPoints)
}

@UseCaseBinding
fun bindEraRewardPoints(
    decoded: Any?
): EraRewardPoints {
    val dynamicInstance = decoded.castToStruct()

    return EraRewardPoints(
        totalPoints = bindRewardPoint(dynamicInstance["total"]),
        individual = dynamicInstance.getList("individual").map {
            requireType<List<*>>(it) // (AccountId, RewardPoint)

            EraRewardPoints.Individual(
                accountId = bindAccountId(it.first()),
                rewardPoints = bindRewardPoint(it.second())
            )
        }
    )
}

@HelperBinding
fun bindRewardPoint(dynamicInstance: Any?): RewardPoints = bindNumber(dynamicInstance)
