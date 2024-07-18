package io.novafoundation.nova.runtime.multiNetwork.chain.model

import io.novafoundation.nova.common.utils.Identifiable

data class LightChain(
    val id: ChainId,
    val name: String,
    val icon: String?
) : Identifiable {

    override val identifier: String = id
}
