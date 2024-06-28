package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.RemoteToDomainChainMapperFacade
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteToDomainLightChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.LightChain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher

interface PreConfiguredChainsRepository {

    suspend fun getPreConfiguredChains(): Result<List<LightChain>>

    suspend fun getPreconfiguredChainById(id: ChainId): Result<Chain>

}

class RealPreConfiguredChainsRepository(
    private val chainFetcher: ChainFetcher,
    private val chainMapperFacade: RemoteToDomainChainMapperFacade
) : PreConfiguredChainsRepository {

    override suspend fun getPreConfiguredChains(): Result<List<LightChain>> {
        return runCatching {
            val remoteChains = chainFetcher.getPreConfiguredChains()
            remoteChains.map { mapRemoteToDomainLightChain(it) }
        }
    }

    override suspend fun getPreconfiguredChainById(id: ChainId): Result<Chain> {
        return runCatching {
            val chain = chainFetcher.getPreConfiguredChainById(id)
            chainMapperFacade.mapRemoteChainToDomain(chain, Chain.Source.CUSTOM)
        }
    }
}
