package io.novafoundation.nova.runtime.multiNetwork.runtime

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.runtimeVersionChange
import jp.co.soramitsu.fearless_utils.wsrpc.subscriptionFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
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
            .catch { Log.e(this@RuntimeVersionSubscription.LOG_TAG, "Failed to sync runtime version for $chainId", it) }
            .launchIn(this)
    }
}
