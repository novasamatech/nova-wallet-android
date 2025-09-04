package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.common.utils.scopeAsync
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.StakingPeriodRewardsResponse
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.totalReward
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.awaitAll

suspend inline fun getAggregatedRewards(
    externalApis: List<Chain.ExternalApi.StakingRewards>,
    crossinline receiver: suspend (String) -> SubQueryResponse<StakingPeriodRewardsResponse>
): BigInteger {
    val urls = externalApis.map { it.url }
    val rewardsDeferredList = urls.map { url ->
        scopeAsync {
            receiver(url)
        }
    }

    return rewardsDeferredList.awaitAll().sumOf {
        it.data.totalReward
    }
}
