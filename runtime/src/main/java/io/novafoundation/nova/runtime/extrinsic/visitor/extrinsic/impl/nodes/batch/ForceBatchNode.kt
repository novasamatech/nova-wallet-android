package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.batch

import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCallList
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.EventCountingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.NestedCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.NestedExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.VisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.indexOfLastOrThrow
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.peekItemFromEndOrThrow
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.takeFromEndOrThrow
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal class ForceBatchNode : NestedCallNode {

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.UTILITY && call.function.name == "force_batch"
    }

    override fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, context: EventCountingContext): Int {
        val innerCalls = bindGenericCallList(call.arguments["calls"])

        val batchCompletedEventType = context.runtime.batchCompletedEvent()
        val batchCompletedWithErrorsType = context.runtime.batchCompletedWithErrorsEvent()

        val itemCompletedEventType = context.runtime.itemCompletedEvent()
        val itemFailedEventType = context.runtime.itemFailedEvent()

        var endExclusive = context.endExclusive

        // Safe since batch all always completes
        val indexOfCompletedEvent = context.eventQueue.indexOfLastOrThrow(batchCompletedEventType, batchCompletedWithErrorsType, endExclusive = endExclusive)
        endExclusive = indexOfCompletedEvent

        innerCalls.reversed().forEach { innerCall ->
            val (itemEvent, itemEventIdx) = context.eventQueue.peekItemFromEndOrThrow(itemCompletedEventType, itemFailedEventType, endExclusive = endExclusive)

            endExclusive = if (itemEvent.instanceOf(itemCompletedEventType)) {
                // only completed items emit nested events
                context.endExclusiveToSkipInternalEvents(innerCall, itemEventIdx)
            } else {
                itemEventIdx
            }
        }

        return endExclusive
    }

    override fun visit(call: GenericCall.Instance, context: VisitingContext) {
        val innerCalls = bindGenericCallList(call.arguments["calls"])

        val batchCompletedEventType = context.runtime.batchCompletedEvent()
        val batchCompletedWithErrorsType = context.runtime.batchCompletedWithErrorsEvent()

        val itemCompletedEventType = context.runtime.itemCompletedEvent()
        val itemFailedEventType = context.runtime.itemFailedEvent()

        context.logger.info("Visiting utility.forceBatch with ${innerCalls.size} inner calls")

        if (context.callSucceeded) {
            context.logger.info("ForceBatch  succeeded")

            context.eventQueue.popFromEnd(batchCompletedEventType, batchCompletedWithErrorsType)
        } else {
            context.logger.info("ForceBatch failed")
        }

        val subItemsToVisit = innerCalls.reversed().map { innerCall ->
            if (context.callSucceeded) {
                val itemEvent = context.eventQueue.takeFromEndOrThrow(itemCompletedEventType, itemFailedEventType)

                if (itemEvent.instanceOf(itemCompletedEventType)) {
                    val allEvents = context.takeCompletedBatchItemEvents(innerCall)

                    return@map NestedExtrinsicVisit(
                        rootExtrinsic = context.rootExtrinsic,
                        call = innerCall,
                        success = true,
                        events = allEvents,
                        origin = context.origin
                    )
                }
            }

            NestedExtrinsicVisit(
                rootExtrinsic = context.rootExtrinsic,
                call = innerCall,
                success = false,
                events = emptyList(),
                origin = context.origin
            )
        }

        subItemsToVisit.forEach { subItem ->
            context.nestedVisit(subItem)
        }
    }
}
