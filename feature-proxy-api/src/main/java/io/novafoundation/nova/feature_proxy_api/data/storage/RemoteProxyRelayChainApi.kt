package io.novafoundation.nova.feature_proxy_api.data.storage

import io.novafoundation.nova.common.utils.remoteProxyRelayChain
import io.novafoundation.nova.feature_proxy_api.data.model.RemoteProxyBlockToRoot
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class RemoteProxyRelayChainApi(override val module: Module) : QueryableModule


context(StorageQueryContext)
val RuntimeMetadata.remoteProxyRelayChain: RemoteProxyRelayChainApi
    get() = RemoteProxyRelayChainApi(remoteProxyRelayChain())

context(StorageQueryContext)
val RemoteProxyRelayChainApi.blockToRoot: QueryableStorageEntry0<List<RemoteProxyBlockToRoot>>
    get() = storage0("BlockToRoot", binding = RemoteProxyBlockToRoot::bindMany)
