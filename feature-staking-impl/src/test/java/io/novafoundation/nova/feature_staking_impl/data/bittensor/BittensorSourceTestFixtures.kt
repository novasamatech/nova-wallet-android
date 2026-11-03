package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow

fun RemoteBittensorConfigSource(api: BittensorConfigApi, configUrl: String, logger: Logger): RemoteBittensorConfigSource {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    val foreground = emptyFlow<Unit>()
    return RemoteBittensorConfigSource(api, configUrl, logger, scope, foreground)
}
