package io.novafoundation.nova.runtime.mapper

import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.chain.RuntimeVersion

fun ChainRuntimeInfoLocal.toRuntimeVersion(): RuntimeVersion? {
    return RuntimeVersion(
        specVersion = this.remoteVersion,
        transactionVersion = this.transactionVersion ?: return null
    )
}
