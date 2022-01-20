package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.common.utils.Modules
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent

private const val SUCCESS_EVENT = "ExtrinsicSuccess"
private const val FAILURE_EVENT = "ExtrinsicFailed"

enum class ExtrinsicStatus {
    SUCCESS, FAILURE, UNKNOWN
}

class ExtrinsicWithEvents(
    val extrinsic: Extrinsic.DecodedInstance,
    val extrinsicHash: String,
    val events: List<GenericEvent.Instance>
)

fun ExtrinsicWithEvents.status(): ExtrinsicStatus {
    return events.mapNotNull {
        when {
            it.module.name == Modules.SYSTEM && it.event.name == SUCCESS_EVENT -> ExtrinsicStatus.SUCCESS
            it.module.name == Modules.SYSTEM && it.event.name == FAILURE_EVENT -> ExtrinsicStatus.FAILURE
            else -> null
        }
    }.firstOrNull() ?: ExtrinsicStatus.UNKNOWN
}
