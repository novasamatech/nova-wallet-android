package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.system
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

class EventRecord(val phase: Phase, val event: GenericEvent.Instance)

sealed class Phase {

    class ApplyExtrinsic(val extrinsicId: BigInteger) : Phase()

    object Finalization : Phase()

    object Initialization : Phase()
}

@HelperBinding
fun bindEventRecord(dynamicInstance: Any?): EventRecord {
    requireType<Struct.Instance>(dynamicInstance)

    val phaseDynamic = dynamicInstance.getTyped<DictEnum.Entry<*>>("phase")

    val phase = when (phaseDynamic.name) {
        "ApplyExtrinsic" -> Phase.ApplyExtrinsic(phaseDynamic.value.cast())
        "Finalization" -> Phase.Finalization
        "Initialization" -> Phase.Initialization
        else -> incompatible()
    }

    val dynamicEvent = dynamicInstance.getTyped<GenericEvent.Instance>("event")

    return EventRecord(phase, dynamicEvent)
}

@UseCaseBinding
fun bindEventRecords(
    scale: String,
    runtime: RuntimeSnapshot,
): List<EventRecord> {
    val returnType = runtime.metadata.system().storage("Events").type.value ?: incompatible()

    val dynamicInstance = returnType.fromHex(runtime, scale)
    requireType<List<*>>(dynamicInstance)

    return dynamicInstance.mapNotNull { dynamicEventRecord ->
        bindOrNull { bindEventRecord(dynamicEventRecord) }
    }
}
