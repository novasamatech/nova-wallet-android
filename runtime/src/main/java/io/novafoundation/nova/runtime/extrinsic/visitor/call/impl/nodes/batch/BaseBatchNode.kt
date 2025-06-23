package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.batch

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCallList
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.BatchCallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.CallVisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.NestedCallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.NestedCallVisitNode
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal abstract class BaseBatchNode : NestedCallVisitNode {

    override fun visit(call: GenericCall.Instance, context: CallVisitingContext) {
        val innerCalls = bindGenericCallList(call.arguments["calls"])

        context.logger.info("Visiting ${this::class.simpleName} with ${innerCalls.size} inner calls")

        val batchVisit = RealBatchCallVisit(call, innerCalls, context.origin)
        context.visit(batchVisit)

        innerCalls.forEach { inner ->
            val nestedVisit = NestedCallVisit(call = inner, origin = context.origin)
            context.nestedVisit(nestedVisit)
        }
    }

    private class RealBatchCallVisit(
        override val call: GenericCall.Instance,
        override val batchedCalls: List<GenericCall.Instance>,
        override val callOrigin: AccountIdKey
    ) : BatchCallVisit
}
