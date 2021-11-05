package io.novafoundation.nova.feature_account_api.data.mappers

import io.novafoundation.nova.core.model.Network
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

fun stubNetwork(chainId: ChainId): Network {
    val networkType = Node.NetworkType.findByGenesis(chainId) ?: Node.NetworkType.POLKADOT

    return Network(networkType)
}
