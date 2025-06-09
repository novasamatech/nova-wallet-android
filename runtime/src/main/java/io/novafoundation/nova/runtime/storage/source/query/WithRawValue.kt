package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class WithRawValue<T>(val at: BlockHash?, val raw: StorageEntry, val chainId: ChainId, val value: T)


data class AtBlock<T>(val value: T, val at: BlockHash)

fun <T> WithRawValue<T>.toAtBlock(): AtBlock<T> {
    return AtBlock(
        at = requireNotNull(at) { "Block hash was not specified" },
        value = value
    )
}
