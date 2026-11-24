package io.novafoundation.nova.feature_swap_impl.data.exchanger

import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.resolve
import java.io.IOException

/**
 * Resolves the configured infrastructure origin for each request.
 * Transport errors and cancellation propagate to the caller.
 */
class ExchangerAQuoteSource(
    private val api: ExchangerAApi,
    private val infrastructureUrls: InfrastructureUrls
) {

    suspend fun assets(): List<ExchangerAAssetRemote> {
        val url = infrastructureUrls.resolve("/v1/exchanger-a/assets")
        val response = api.assets(url)
        val assets = response.assets ?: throw IOException("Missing Exchanger A assets")
        return assets.filterNotNull()
    }

    suspend fun quote(request: ExchangerAQuoteRequest): ExchangerAQuoteRemote {
        val url = infrastructureUrls.resolve("/v1/exchanger-a/quotes")
        return api.quote(url, request)
    }
}
