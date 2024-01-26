package io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.batch

import io.novafoundation.nova.common.utils.utility
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.VisitingContext
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.metadata.event
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Event

internal fun RuntimeSnapshot.batchCompletedEvent(): Event {
    return metadata.utility().event("BatchCompleted")
}

internal fun RuntimeSnapshot.itemCompletedEvent(): Event {
    return metadata.utility().event("ItemCompleted")
}

internal fun RuntimeSnapshot.itemFailedEvent(): Event {
    return metadata.utility().event("ItemFailed")
}

internal fun VisitingContext.takeCompletedBatchItemEvents(call: GenericCall.Instance): List<GenericEvent.Instance> {
    val internalEventsEndExclusive = endExclusiveToSkipInternalEvents(call)

    // internalEnd is exclusive => it holds index of last internal event
    // thus, we delete them inclusively
    val someOfNestedEvents = eventQueue.takeAllAfterInclusive(internalEventsEndExclusive)

    // now it is safe to go until ItemCompleted\ItemFailed since we removed all potential nested events above
    val remainingNestedEvents = eventQueue.takeTail(runtime.itemCompletedEvent(), runtime.itemFailedEvent())

    return remainingNestedEvents + someOfNestedEvents
}
