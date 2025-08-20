package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.runtime.extrinsic.visitor.ExtrinsicVisitorLogger
import io.novafoundation.nova.runtime.extrinsic.visitor.IndentVisitorLogger
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallTraversal
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisitor
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.LeafCallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.batch.BatchAllCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.batch.BatchCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.batch.ForceBatchCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.multisig.MultisigCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.proxy.ProxyCallNode
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress

internal class RealCallTraversal(
    private val knownNodes: List<NestedCallVisitNode> = defaultNodes(),
) : CallTraversal {

    companion object {

        fun defaultNodes(): List<NestedCallVisitNode> = listOf(
            BatchCallNode(),
            BatchAllCallNode(),
            ForceBatchCallNode(),

            ProxyCallNode(),

            MultisigCallNode()
        )
    }

    override fun traverse(
        source: GenericCall.Instance,
        initialOrigin: AccountIdKey,
        visitor: CallVisitor
    ) {
        val rootVisit = NestedCallVisit(
            call = source,
            origin = initialOrigin
        )

        nestedVisit(visitor, rootVisit)
    }

    private fun nestedVisit(
        visitor: CallVisitor,
        visitedCall: NestedCallVisit
    ) {
        val nestedNode = findNestedNode(visitedCall.call)

        if (nestedNode == null) {
            val publicVisit = visitedCall.toLeafVisit()

            val call = visitedCall.call
            val display = "${call.module.name}.${call.function.name}"
            val origin = visitedCall.origin
            val newLogger = IndentVisitorLogger()

            newLogger.info("Visited leaf: $display, origin: ${origin.value.toAddress(42)}")

            visitor.visit(publicVisit)
        } else {
            val newLogger = IndentVisitorLogger()

            val context = RealCallVisitingContext(
                origin = visitedCall.origin,
                visitor = visitor,
                logger = newLogger,
            )

            nestedNode.visit(visitedCall.call, context)
        }
    }

    private fun findNestedNode(call: GenericCall.Instance): NestedCallVisitNode? {
        return knownNodes.find { it.canVisit(call) }
    }

    private fun NestedCallVisit.toLeafVisit(): CallVisit {
        return LeafCallVisit(call, origin)
    }

    private inner class RealCallVisitingContext(
        override val origin: AccountIdKey,
        override val logger: ExtrinsicVisitorLogger,
        private val visitor: CallVisitor
    ) : CallVisitingContext {

        override fun nestedVisit(visit: NestedCallVisit) {
            return this@RealCallTraversal.nestedVisit(visitor, visit)
        }

        override fun visit(visit: CallVisit) {
            visitor.visit(visit)
        }
    }
}
