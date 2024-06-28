package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.LightChain
import io.novafoundation.nova.runtime.repository.PreConfiguredChainsRepository

interface PreConfiguredNetworksInteractor {

    suspend fun getPreConfiguredNetworks(): Result<List<LightChain>>

    suspend fun excludeChains(list: List<LightChain>, chainIds: Set<String>): List<LightChain>

    fun searchNetworks(query: String?, list: List<LightChain>): List<LightChain>

    suspend fun getPreConfiguredNetwork(chainId: String): Result<Chain>
}

class RealPreConfiguredNetworksInteractor(
    private val preConfiguredChainsRepository: PreConfiguredChainsRepository
) : PreConfiguredNetworksInteractor {

    override suspend fun getPreConfiguredNetworks(): Result<List<LightChain>> {
        return preConfiguredChainsRepository.getPreConfiguredChains()
            .map { lightChains ->
                lightChains.sortedBy { it.name }
            }
    }

    override suspend fun excludeChains(list: List<LightChain>, chainIds: Set<String>): List<LightChain> {
        return list.filterNot { lightChain -> chainIds.contains(lightChain.id) }
    }

    override fun searchNetworks(query: String?, list: List<LightChain>): List<LightChain> {
        if (query.isNullOrBlank()) return list

        val loverCaseQuery = query.trim().lowercase()

        return list.filter { lightChain -> lightChain.name.lowercase().startsWith(loverCaseQuery) }
    }

    override suspend fun getPreConfiguredNetwork(chainId: String): Result<Chain> {
        return preConfiguredChainsRepository.getPreconfiguredChainById(chainId)
    }
}
