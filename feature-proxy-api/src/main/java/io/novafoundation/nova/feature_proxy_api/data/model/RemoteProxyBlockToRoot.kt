package io.novafoundation.nova.feature_proxy_api.data.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList

class RemoteProxyBlockToRoot(
    val block: BlockNumber,
    val stateRoot: ByteArray
) {

    companion object {

        fun bind(decoded: Any?): RemoteProxyBlockToRoot {
            val (blockNumberRaw, stateRootRaw) = decoded.castToList()

            return RemoteProxyBlockToRoot(
                block = bindNumber(blockNumberRaw),
                stateRoot = bindByteArray(stateRootRaw)
            )
        }

        fun bindMany(decoded: Any?): List<RemoteProxyBlockToRoot> {
            return bindList(decoded, ::bind)
        }
    }
}
