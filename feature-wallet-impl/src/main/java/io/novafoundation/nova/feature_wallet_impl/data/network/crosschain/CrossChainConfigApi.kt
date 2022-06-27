package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.feature_wallet_impl.BuildConfig
import retrofit2.http.GET

interface CrossChainConfigApi {

    @GET(BuildConfig.CROSS_CHAIN_CONFIG_URL)
    suspend fun getCrossChainConfig(): String
}
