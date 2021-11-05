package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
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
