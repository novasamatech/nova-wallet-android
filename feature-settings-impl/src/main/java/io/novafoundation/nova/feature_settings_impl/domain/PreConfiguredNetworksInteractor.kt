package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class PreConfiguredNetwork(
    val chainId: String,
    val name: String,
    val iconUrl: String?
)

interface PreConfiguredNetworksInteractor {

    suspend fun getPreConfiguredNetworks(): List<PreConfiguredNetwork>

    fun filterNetworks(query: String, list: List<PreConfiguredNetwork>): List<PreConfiguredNetwork>

    suspend fun getPreConfiguredNetwork(chainId: String): Chain
}

class RealPreConfiguredNetworksInteractor() : PreConfiguredNetworksInteractor {

    override suspend fun getPreConfiguredNetworks(): List<PreConfiguredNetwork> {
        TODO("Filter all networks that already added in the app")
    }

    override fun filterNetworks(query: String, list: List<PreConfiguredNetwork>): List<PreConfiguredNetwork> {

    }

    override suspend fun getPreConfiguredNetwork(chainId: String): Chain {

    }
}
