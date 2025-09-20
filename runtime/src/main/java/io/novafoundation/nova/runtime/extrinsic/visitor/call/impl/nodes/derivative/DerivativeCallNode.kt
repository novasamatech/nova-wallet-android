package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.derivative

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.DerivativeVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.CallVisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.NestedCallVisitNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nestedVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.derivative.generateDerivativeAddress
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal class DerivativeCallNode : NestedCallVisitNode {

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.UTILITY && call.function.name == "as_derivative"
    }

    override fun visit(call: GenericCall.Instance, context: CallVisitingContext) {
        context.logger.info("Visiting derivative")

        val derivativeIndex = extractDerivativeIndex(call)
        val innerCall = extractInnerCall(call)

        val multisigVisit = RealDerivativeVisit(
            call = call,
            callOrigin = context.origin,
            index = derivativeIndex,
            nestedCall = innerCall
        )

        context.visit(multisigVisit)
        context.nestedVisit(multisigVisit.nestedCall, multisigVisit.derivative)
    }

    private fun extractInnerCall(multisigCall: GenericCall.Instance): GenericCall.Instance {
        return bindGenericCall(multisigCall.arguments["call"])
    }

    private fun extractDerivativeIndex(call: GenericCall.Instance): Int {
        return bindInt(call.arguments["index"])
    }

    private class RealDerivativeVisit(
        override val call: GenericCall.Instance,
        override val callOrigin: AccountIdKey,
        override val index: Int,
        override val nestedCall: GenericCall.Instance
    ) : DerivativeVisit {

        override val derivative: AccountIdKey = generateDerivativeAddress(
            parent = callOrigin,
            index = index
        )
    }
}
