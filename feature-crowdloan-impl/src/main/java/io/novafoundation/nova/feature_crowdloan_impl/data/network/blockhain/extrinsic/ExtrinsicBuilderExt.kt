package io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic

import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

fun ExtrinsicBuilder.contribute(parachainId: ParaId, contribution: BigInteger): ExtrinsicBuilder {
    return call(
        moduleName = "Crowdloan",
        callName = "contribute",
        arguments = mapOf(
            "index" to parachainId,
            "value" to contribution,
            "signature" to null // do not support private crowdloans yet
        )
    )
}

fun ExtrinsicBuilder.addMemo(parachainId: ParaId, memo: String): ExtrinsicBuilder {
    return call(
        moduleName = "Crowdloan",
        callName = "add_memo",
        arguments = mapOf(
            "index" to parachainId,
            "memo" to memo.toByteArray()
        )
    )
}
