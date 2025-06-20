package io.novafoundation.nova.runtime.ethereum.gas

import io.novafoundation.nova.runtime.ethereum.gas.etherscan.EtherscanGasApi
import io.novafoundation.nova.runtime.ethereum.gas.etherscan.EtherscanGasPriceProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.network.etherscan.EtherscanApiKeys
import org.web3j.protocol.Web3j

interface GasPriceProviderFactory {

    /**
     * Creates gas provider for a [chain] that is known to the app
     */
    suspend fun createKnown(chain: Chain): GasPriceProvider

    /**
     * Creates gas provider for arbitrary EVM chain given instance of [Web3j]
     */
    suspend fun create(web3j: Web3j): GasPriceProvider
}

class RealGasPriceProviderFactory(
    private val chainRegistry: ChainRegistry,
    private val api: EtherscanGasApi,
    private val etherscanApiKeys: EtherscanApiKeys,
) : GasPriceProviderFactory {

    override suspend fun createKnown(chain: Chain): GasPriceProvider {
        val api = chainRegistry.getCallEthereumApiOrThrow(chain.id)

        return create(api, chain)
    }

    override suspend fun create(web3j: Web3j): GasPriceProvider {
        return create(web3j, chain = null)
    }

    private fun create(web3j: Web3j, chain: Chain?) : GasPriceProvider {
        val providers = listOfNotNull(
            chain?.let { EtherscanGasPriceProvider(api, etherscanApiKeys, chain) },
            MaxPriorityFeeGasProvider(web3j),
            LegacyGasPriceProvider(web3j)
        )

        return CompoundGasPriceProvider(providers)
    }
}
