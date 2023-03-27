package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan

import io.novafoundation.nova.feature_wallet_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.Ids
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class EtherscanApiKeys(private val keys: Map<ChainId, String>) {

    companion object {

        fun default(): EtherscanApiKeys {
            return EtherscanApiKeys(
                mapOf(
                    Chain.Ids.MOONBEAM to BuildConfig.EHTERSCAN_API_KEY_MOONBEAM,
                    Chain.Ids.MOONRIVER to BuildConfig.EHTERSCAN_API_KEY_MOONRIVER,
                    Chain.Ids.ETHEREUM to BuildConfig.EHTERSCAN_API_KEY_ETHEREUM
                )
            )
        }
    }

    fun keyFor(chainId: ChainId): String? = keys[chainId]
}
