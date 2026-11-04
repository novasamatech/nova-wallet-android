package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import kotlinx.coroutines.flow.Flow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class BittensorPortfolioHistorySource(
    private val client: BittensorBackendClient,
    private val portfolioHistoryApi: BittensorPortfolioHistoryApi
) {

    fun observePortfolioHistory(
        request: BittensorPortfolioHistoryRequest,
        address: String
    ): Flow<BittensorPortfolioHistoryRemote> = client.observe(
        url = { portfolioHistoryUrl(request, address) },
        type = BittensorPortfolioHistoryRemote::class.java,
        metaAccountId = request.metaId,
        validate = { mapBittensorPortfolioHistory(it, request.period) },
        fetch = portfolioHistoryApi::history
    )

    private suspend fun portfolioHistoryUrl(request: BittensorPortfolioHistoryRequest, address: String): String {
        require(address.length in 1..128 && address.all { it in '!'..'~' }) { "Invalid account subject" }
        return client.url(PATH).toHttpUrl().newBuilder()
            .addQueryParameter("accountSubject", address)
            .addQueryParameter("period", request.period.name)
            .build().toString()
    }

    suspend fun invalidate(base: HttpUrl, address: String, metaAccountId: Long) {
        client.invalidateAccountResponses(base, address, metaAccountId, PATH)
    }

    private companion object {
        const val PATH = "portfolio/history/search"
    }
}
