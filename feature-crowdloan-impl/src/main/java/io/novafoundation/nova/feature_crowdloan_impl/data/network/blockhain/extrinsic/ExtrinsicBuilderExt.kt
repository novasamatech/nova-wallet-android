package io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

fun ExtrinsicBuilder.contribute(
    parachainId: ParaId,
    contribution: BigInteger,
    signature: Any?,
): ExtrinsicBuilder {
    return call(
        moduleName = "Crowdloan",
        callName = "contribute",
        arguments = mapOf(
            "index" to parachainId,
            "value" to contribution,
            "signature" to signature
        )
    )
}

fun ExtrinsicBuilder.addMemo(parachainId: ParaId, memo: String): ExtrinsicBuilder {
    return addMemo(parachainId, memo.toByteArray())
}

fun ExtrinsicBuilder.addMemo(parachainId: ParaId, memo: ByteArray): ExtrinsicBuilder {
    return call(
        moduleName = "Crowdloan",
        callName = "add_memo",
        arguments = mapOf(
            "index" to parachainId,
            "memo" to memo
        )
    )
}
