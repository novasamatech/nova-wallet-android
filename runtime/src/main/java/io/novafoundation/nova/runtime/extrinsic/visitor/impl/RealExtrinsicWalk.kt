package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisitor
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.batch.BatchAllNode
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.proxy.ProxyNode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.isSuccess
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.signer
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAddress

internal class RealExtrinsicWalk(
    private val chainRegistry: ChainRegistry,
    private val knownNodes: List<NestedCallNode> = defaultNodes(),
) : ExtrinsicWalk {

    companion object {

        fun defaultNodes() = listOf(ProxyNode(), BatchAllNode())
    }

    override suspend fun walk(
        source: ExtrinsicWithEvents,
        chainId: ChainId,
        visitor: ExtrinsicVisitor
    ) {
        val runtime = chainRegistry.getRuntime(chainId)

        val rootVisit = ExtrinsicVisit(
            rootExtrinsic = source,
            call = source.extrinsic.call,
            success = source.isSuccess(),
            events = source.events,
            origin = source.extrinsic.signer()
        )

        nestedVisit(runtime, visitor, rootVisit, depth = 0)
    }

    private fun nestedVisit(
        runtime: RuntimeSnapshot,
        visitor: ExtrinsicVisitor,
        visitedCall: ExtrinsicVisit,
        depth: Int,
    ) {
        val nestedNode = findNestedNode(visitedCall.call)

        if (nestedNode == null) {
            val call = visitedCall.call
            val display = "${call.module.name}.${call.function.name}"
            val origin = visitedCall.origin
            val newLogger = IndentVisitorLogger(indent = depth + 1)

            newLogger.info("Visiting leaf: $display, success: ${visitedCall.success}, origin: ${origin.toAddress(42)}")

            visitor.visit(visitedCall)
        } else {
            val eventQueue = RealEventQueue(visitedCall.events)
            val newLogger = IndentVisitorLogger(indent = depth)

            val context = RealVisitingContext(
                rootExtrinsic = visitedCall.rootExtrinsic,
                eventsSize = visitedCall.events.size,
                depth = depth,
                runtime = runtime,
                origin = visitedCall.origin,
                callSucceeded = visitedCall.success,
                visitor = visitor,
                logger = newLogger,
                eventQueue = eventQueue
            )

            nestedNode.visit(visitedCall.call, context)
        }
    }

    private fun endExclusiveToSkipInternalEvents(
        runtime: RuntimeSnapshot,
        call: GenericCall.Instance,
        eventQueue: EventQueue,
        endExclusive: Int,
    ): Int {
        val nestedNode = this.findNestedNode(call)

        return if (nestedNode != null) {
            val context: EventCountingContext = RealEventCountingContext(runtime, eventQueue, endExclusive)

            nestedNode.endExclusiveToSkipInternalEvents(call, context)
        } else {
            // no internal events to skip since its a leaf
            endExclusive
        }
    }

    private fun findNestedNode(call: GenericCall.Instance): NestedCallNode? {
        return knownNodes.find { it.canVisit(call) }
    }

    private inner class RealVisitingContext(
        private val eventsSize: Int,
        private val depth: Int,
        override val rootExtrinsic: ExtrinsicWithEvents,
        override val runtime: RuntimeSnapshot,
        override val origin: AccountId,
        override val callSucceeded: Boolean,
        override val visitor: ExtrinsicVisitor,
        override val logger: ExtrinsicVisitorLogger,
        override val eventQueue: MutableEventQueue
    ) : VisitingContext {

        override fun nestedVisit(visit: ExtrinsicVisit) {
            return this@RealExtrinsicWalk.nestedVisit(runtime, visitor, visit, depth + 1)
        }

        override fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance): Int {
            return this@RealExtrinsicWalk.endExclusiveToSkipInternalEvents(runtime, call, eventQueue, endExclusive = eventsSize)
        }
    }

    private inner class RealEventCountingContext(
        override val runtime: RuntimeSnapshot,
        override val eventQueue: EventQueue,
        override val endExclusive: Int
    ) : EventCountingContext {

        override fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, endExclusive: Int): Int {
            return this@RealExtrinsicWalk.endExclusiveToSkipInternalEvents(runtime, call, eventQueue, endExclusive)
        }
    }
}
