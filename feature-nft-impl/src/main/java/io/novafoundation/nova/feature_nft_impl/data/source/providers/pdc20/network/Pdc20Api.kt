package io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface Pdc20Api {

    companion object {
        const val NETWORK_POLKADOT = "polkadot"
    }

    @POST("https://squid.subsquid.io/dot-ordinals/graphql")
    suspend fun getNfts(@Body request: Pdc20Request): SubQueryResponse<Pdc20NftResponse>
}
