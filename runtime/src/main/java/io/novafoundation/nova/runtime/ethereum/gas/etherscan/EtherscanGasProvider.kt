package io.novafoundation.nova.runtime.ethereum.gas.etherscan

import io.novafoundation.nova.common.utils.gWeiToPlanks
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProvider
import io.novafoundation.nova.runtime.ext.evmTransfersApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.etherscan.EtherscanApiKeys
import java.math.BigInteger

class EtherscanGasPriceProvider(
    private val api: EtherscanGasApi,
    private val etherscanApiKeys: EtherscanApiKeys,
    private val chain: Chain
): GasPriceProvider {

    override suspend fun getGasPrice(): BigInteger {
        val apiUrl = chain.evmTransfersApi()?.url ?: error("Evm transfers api not found for ${chain.name}")
        val apiKey = etherscanApiKeys.keyFor(chain.id) ?: error("Etherscan api key not found for ${chain.name}")
        val response = api.getGasEstimate(apiUrl, apiKey)
        val totalGasPrice = response.result.suggestBaseFee + response.result.proposeGasPrice
        return totalGasPrice.gWeiToPlanks()
    }
}
