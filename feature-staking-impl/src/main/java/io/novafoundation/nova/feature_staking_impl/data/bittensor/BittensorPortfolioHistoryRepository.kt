package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioStakeHistory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import retrofit2.HttpException

interface BittensorPortfolioHistoryRepository {

    suspend fun history(chain: Chain, coldkey: AccountIdKey, period: BittensorPortfolioHistoryPeriod): BittensorPortfolioStakeHistory
}

/** The account subject is the coldkey as SS58 with the chain prefix (42 on Bittensor), the generic format iOS sends. */
class RealBittensorPortfolioHistoryRepository(
    private val api: BittensorPortfolioHistoryApi,
    private val baseUrl: String
) : BittensorPortfolioHistoryRepository {

    override suspend fun history(chain: Chain, coldkey: AccountIdKey, period: BittensorPortfolioHistoryPeriod): BittensorPortfolioStakeHistory {
        val request = BittensorPortfolioHistoryRequest(chain.addressOf(coldkey), period.wireName)
        val response = api.history(bittensorBackendUrl(baseUrl, "portfolio/history/search"), request)
        if (!response.isSuccessful) throw HttpException(response)

        return bindBittensorPortfolioHistory(requireNotNull(response.body()) { "Empty Bittensor response" }, period)
    }
}
