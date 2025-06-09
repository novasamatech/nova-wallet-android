package io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.batch

import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCallList
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.EventCountingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.NestedCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.NestedExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.VisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.indexOfLastOrThrow
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal class BatchAllNode : NestedCallNode {

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.UTILITY && call.function.name == "batch_all"
    }

    override fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, context: EventCountingContext): Int {
        val innerCalls = bindGenericCallList(call.arguments["calls"])

        val batchCompletedEventType = context.runtime.batchCompletedEvent()
        val itemCompletedEventType = context.runtime.itemCompletedEvent()

        var endExclusive = context.endExclusive

        // Safe since `endExclusiveToSkipInternalEvents` should not be called on failed items
        val indexOfCompletedEvent = context.eventQueue.indexOfLastOrThrow(batchCompletedEventType, endExclusive = endExclusive)
        endExclusive = indexOfCompletedEvent

        innerCalls.reversed().forEach { innerCall ->
            val itemIdx = context.eventQueue.indexOfLastOrThrow(itemCompletedEventType, endExclusive = endExclusive)
            endExclusive = context.endExclusiveToSkipInternalEvents(innerCall, itemIdx)
        }

        return endExclusive
    }

    override fun visit(call: GenericCall.Instance, context: VisitingContext) {
        val innerCalls = bindGenericCallList(call.arguments["calls"])
        val itemCompletedEventType = context.runtime.itemCompletedEvent()

        context.logger.info("Visiting utility.batchAll with ${innerCalls.size} inner calls")

        if (context.callSucceeded) {
            context.logger.info("BatchAll succeeded")
        } else {
            context.logger.info("BatchAll failed")
        }

        val subItemsToVisit = innerCalls.reversed().map { innerCall ->
            if (context.callSucceeded) {
                context.eventQueue.popFromEnd(itemCompletedEventType)
                val alNestedEvents = context.takeCompletedBatchItemEvents(innerCall)

                NestedExtrinsicVisit(
                    rootExtrinsic = context.rootExtrinsic,
                    call = innerCall,
                    success = true,
                    events = alNestedEvents,
                    origin = context.origin
                )
            } else {
                NestedExtrinsicVisit(
                    rootExtrinsic = context.rootExtrinsic,
                    call = innerCall,
                    success = false,
                    events = emptyList(),
                    origin = context.origin
                )
            }
        }

        subItemsToVisit.forEach { subItem ->
            context.nestedVisit(subItem)
        }
    }
}
