package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import io.novafoundation.nova.runtime.multiNetwork.chain.model.LightChain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.LightChainRemote

fun mapRemoteToDomainLightChain(chain: LightChainRemote): LightChain {
    return LightChain(
        id = chain.chainId,
        name = chain.name,
        icon = chain.icon,
    )
}
