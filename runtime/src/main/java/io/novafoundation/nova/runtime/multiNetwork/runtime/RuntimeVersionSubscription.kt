package io.novafoundation.nova.runtime.multiNetwork.runtime

import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.runtimeVersionChange
import jp.co.soramitsu.fearless_utils.wsrpc.subscriptionFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class RuntimeVersionSubscription(
    private val chainId: String,
    connection: ChainConnection,
    private val chainDao: ChainDao,
    private val runtimeSyncService: RuntimeSyncService,
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    init {
        connection.socketService.subscriptionFlow(SubscribeRuntimeVersionRequest)
            .map { it.runtimeVersionChange().specVersion }
            .onEach { runtimeVersion ->
                chainDao.updateRemoteRuntimeVersionIfChainExists(chainId, runtimeVersion)

                runtimeSyncService.applyRuntimeVersion(chainId)
            }
            .launchIn(this)
    }
}
