package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl

import io.novafoundation.nova.runtime.extrinsic.visitor.ExtrinsicVisitorLogger
import io.novafoundation.nova.runtime.extrinsic.visitor.IndentVisitorLogger
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisitor
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.batch.BatchAllNode
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.batch.ForceBatchNode
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.multisig.MultisigNode
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.proxy.ProxyNode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.isSuccess
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.signer
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress

internal class RealExtrinsicWalk(
    private val chainRegistry: ChainRegistry,
    private val knownNodes: List<NestedCallNode> = defaultNodes(),
) : ExtrinsicWalk {

    companion object {

        fun defaultNodes() = listOf(
            ProxyNode(),

            BatchAllNode(),
            ForceBatchNode(),

            MultisigNode()
        )
    }

    override suspend fun walk(
        source: ExtrinsicWithEvents,
        chainId: ChainId,
        visitor: ExtrinsicVisitor
    ) {
        val runtime = chainRegistry.getRuntime(chainId)

        val rootVisit = NestedExtrinsicVisit(
            rootExtrinsic = source,
            call = source.extrinsic.call,
            success = source.isSuccess(),
            events = source.events,
            origin = source.extrinsic.signer() ?: error("Unsigned extrinsic"),
        )

        nestedVisit(runtime, visitor, rootVisit, depth = 0)
    }

    private fun nestedVisit(
        runtime: RuntimeSnapshot,
        visitor: ExtrinsicVisitor,
        visitedCall: NestedExtrinsicVisit,
        depth: Int,
    ) {
        val nestedNode = findNestedNode(visitedCall.call)
        val publicVisit = visitedCall.toVisit(hasRegisteredNode = nestedNode != null)

        visitor.visit(publicVisit)

        if (nestedNode == null) {
            val call = visitedCall.call
            val display = "${call.module.name}.${call.function.name}"
            val origin = visitedCall.origin
            val newLogger = IndentVisitorLogger(indent = depth + 1)

            newLogger.info("Visited leaf: $display, success: ${visitedCall.success}, origin: ${origin.toAddress(42)}")
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

    private fun NestedExtrinsicVisit.toVisit(hasRegisteredNode: Boolean): ExtrinsicVisit {
        return ExtrinsicVisit(rootExtrinsic, call, success, events, origin, hasRegisteredNode)
    }

    private inner class RealVisitingContext(
        private val eventsSize: Int,
        private val depth: Int,
        override val rootExtrinsic: ExtrinsicWithEvents,
        override val runtime: RuntimeSnapshot,
        override val origin: AccountId,
        override val callSucceeded: Boolean,
        override val logger: ExtrinsicVisitorLogger,
        override val eventQueue: MutableEventQueue,
        private val visitor: ExtrinsicVisitor
    ) : VisitingContext {

        override fun nestedVisit(visit: NestedExtrinsicVisit) {
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
