package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.MultisigCallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.CallVisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.NestedCallVisitNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nestedVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.multisig.generateMultisigAddress
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal class MultisigCallNode : NestedCallVisitNode {

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.MULTISIG && call.function.name == "as_multi"
    }

    override fun visit(call: GenericCall.Instance, context: CallVisitingContext) {
        context.logger.info("Visiting multisig")

        val innerOriginInfo = extractMultisigOriginInfo(call)
        val innerCall = extractInnerMultisigCall(call)

        val multisigVisit = RealMultisigCallVisit(
            call = call,
            callOrigin = context.origin,
            otherSignatories = innerOriginInfo.otherSignatories,
            threshold = innerOriginInfo.threshold,
            nestedCall = innerCall
        )

        context.visit(multisigVisit)
        context.nestedVisit(multisigVisit.nestedCall, multisigVisit.multisig)
    }

    private fun extractInnerMultisigCall(multisigCall: GenericCall.Instance): GenericCall.Instance {
        return bindGenericCall(multisigCall.arguments["call"])
    }

    private fun extractMultisigOriginInfo(call: GenericCall.Instance): MultisigOriginInfo {
        val threshold = bindInt(call.arguments["threshold"])
        val otherSignatories = bindList(call.arguments["other_signatories"], ::bindAccountIdKey)

        return MultisigOriginInfo(threshold, otherSignatories)
    }

    private class MultisigOriginInfo(
        val threshold: Int,
        val otherSignatories: List<AccountIdKey>,
    )

    private class RealMultisigCallVisit(
        override val call: GenericCall.Instance,
        override val callOrigin: AccountIdKey,
        override val otherSignatories: List<AccountIdKey>,
        override val threshold: Int,
        override val nestedCall: GenericCall.Instance,
    ) : MultisigCallVisit {

        override val multisig: AccountIdKey = generateMultisigAddress(
            signatory = callOrigin,
            otherSignatories = otherSignatories,
            threshold = threshold
        )
    }
}
