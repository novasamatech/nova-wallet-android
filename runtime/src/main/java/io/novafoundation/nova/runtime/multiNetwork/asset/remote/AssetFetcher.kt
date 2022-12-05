package io.novafoundation.nova.runtime.multiNetwork.asset.remote

import io.novafoundation.nova.runtime.BuildConfig
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.model.EVMAssetRemote
import retrofit2.http.GET

interface AssetFetcher {

    @GET(BuildConfig.EVM_ASSETS_URL)
    suspend fun getEVMAssets(): List<EVMAssetRemote>
}
