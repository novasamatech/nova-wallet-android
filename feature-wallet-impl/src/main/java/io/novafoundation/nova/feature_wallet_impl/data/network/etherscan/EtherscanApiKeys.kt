package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan

import io.novafoundation.nova.feature_wallet_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class EtherscanApiKeys(private val keys: Map<ChainId, String>) {

    companion object {

        fun default(): EtherscanApiKeys {
            return EtherscanApiKeys(mapOf(
                Chain.Geneses.MOONBEAM to BuildConfig.EHTERSCAN_API_KEY_MOONBEAM,
                Chain.Geneses.MOONRIVER to BuildConfig.EHTERSCAN_API_KEY_MOONRIVER
            ))
        }
    }
    
    fun keyFor(chainId: ChainId): String? = keys[chainId]
}
