package io.novafoundation.nova.runtime.multiNetwork.chain.remote

import io.novafoundation.nova.runtime.BuildConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.LightChainRemote
import retrofit2.http.GET
import retrofit2.http.Path

interface ChainFetcher {

    @GET(BuildConfig.CHAINS_URL)
    suspend fun getChains(): List<ChainRemote>

    @GET(BuildConfig.PRE_CONFIGURED_CHAINS_URL)
    suspend fun getPreConfiguredChains(): List<LightChainRemote>

    @GET(BuildConfig.PRE_CONFIGURED_CHAIN_DETAILS_URL + "/{id}.json")
    suspend fun getPreConfiguredChainById(@Path("id") id: String): ChainRemote
}
