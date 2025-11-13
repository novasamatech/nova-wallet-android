package io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
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

fun ExtrinsicBuilder.claimContribution(parachainId: ParaId, block: BlockNumber, depositor: AccountId) {
    call(
        moduleName = "AhOps",
        callName = "withdraw_crowdloan_contribution",
        arguments = mapOf(
            "block" to block,
            "para_id" to parachainId,
            "depositor" to depositor
        )
    )
}
