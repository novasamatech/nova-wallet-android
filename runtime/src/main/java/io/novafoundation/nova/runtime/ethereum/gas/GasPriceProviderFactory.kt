package io.novafoundation.nova.runtime.ethereum.gas

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.awaitCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import org.web3j.protocol.Web3j

interface GasPriceProviderFactory {

    /**
     * Creates gas provider for a [chainId] that is known to the app
     */
    suspend fun createKnown(chainId: ChainId): GasPriceProvider

    /**
     * Creates gas provider for arbitrary EVM chain given instance of [Web3j]
     */
    suspend fun create(web3j: Web3j): GasPriceProvider
}

class RealGasPriceProviderFactory(
    private val chainRegistry: ChainRegistry
) : GasPriceProviderFactory {

    override suspend fun createKnown(chainId: ChainId): GasPriceProvider {
        val api = chainRegistry.awaitCallEthereumApiOrThrow(chainId)

        return create(api)
    }

    override suspend fun create(web3j: Web3j): GasPriceProvider {
        return CompoundGasPriceProvider(
            MaxPriorityFeeGasProvider(web3j),
            LegacyGasPriceProvider(web3j)
        )
    }
}
