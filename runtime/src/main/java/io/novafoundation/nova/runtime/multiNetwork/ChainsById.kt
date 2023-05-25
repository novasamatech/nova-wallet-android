package io.novafoundation.nova.runtime.multiNetwork

import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

@JvmInline
value class ChainsById(val value: Map<ChainId, Chain>) : Map<ChainId, Chain> by value {

    override operator fun get(key: ChainId): Chain? {
        return value[key.removeHexPrefix()]
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Map<ChainId, Chain>.asChainsById(): ChainsById {
    return ChainsById(this)
}
