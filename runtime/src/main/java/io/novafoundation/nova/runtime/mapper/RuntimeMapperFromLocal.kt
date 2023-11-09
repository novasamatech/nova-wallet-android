package io.novafoundation.nova.runtime.mapper

import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion

fun ChainRuntimeInfoLocal.toRuntimeVersion(): RuntimeVersion {
    return RuntimeVersion(
        specVersion = this.remoteVersion,
        transactionVersion = this.transactionVersion
    )
}
