package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import java.math.BigInteger

private const val SUCCESS_EVENT = "ExtrinsicSuccess"
private const val FAILURE_EVENT = "ExtrinsicFailed"

enum class ExtrinsicStatus {
    SUCCESS, FAILURE
}

class ExtrinsicWithEvents(
    val extrinsic: Extrinsic.DecodedInstance,
    val extrinsicHash: String,
    val events: List<GenericEvent.Instance>
)

fun ExtrinsicWithEvents.status(): ExtrinsicStatus? {
    return events.firstNotNullOfOrNull {
        when {
            it.instanceOf(Modules.SYSTEM, SUCCESS_EVENT) -> ExtrinsicStatus.SUCCESS
            it.instanceOf(Modules.SYSTEM, FAILURE_EVENT) -> ExtrinsicStatus.FAILURE
            else -> null
        }
    }
}

fun ExtrinsicWithEvents.nativeFee(): BigInteger? {
    val event = findEvent(Modules.TRANSACTION_PAYMENT, "TransactionFeePaid") ?: return null
    val (_, actualFee, tip) = event.arguments

    return bindNumber(actualFee) + bindNumber(tip)
}

fun ExtrinsicWithEvents.findEvent(module: String, event: String): GenericEvent.Instance? {
    return events.find { it.instanceOf(module, event) }
}
