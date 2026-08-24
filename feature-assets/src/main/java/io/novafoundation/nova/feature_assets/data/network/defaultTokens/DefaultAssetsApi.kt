package io.novafoundation.nova.feature_assets.data.network.defaultTokens

import retrofit2.http.GET
import retrofit2.http.Url

interface DefaultAssetsApi {

    @GET
    suspend fun getDefaultAssets(@Url url: String): List<DefaultAssetRemote>
}
