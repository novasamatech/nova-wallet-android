package io.novafoundation.nova.web3names.data.provider

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface Web3NamesServiceChainIdProvider {
    fun getChainId(): ChainId
}

class RealWeb3NamesServiceChainIdProvider(private val chainId: ChainId) : Web3NamesServiceChainIdProvider {

    override fun getChainId(): ChainId {
        return chainId
    }
}
