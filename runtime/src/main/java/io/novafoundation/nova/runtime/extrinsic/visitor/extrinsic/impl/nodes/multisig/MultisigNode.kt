package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.multisig
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.EventCountingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.NestedCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.NestedExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.VisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.peekItemFromEndOrThrow
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.takeFromEndOrThrow
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.event
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event

internal class MultisigNode : NestedCallNode {

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.MULTISIG && call.function.name == "as_multi"
    }

    override fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, context: EventCountingContext): Int {
        var endExclusive = context.endExclusive
        val completionEventTypes = context.runtime.multisigCompletionEvents()

        val (completionEvent, completionIdx) = context.eventQueue.peekItemFromEndOrThrow(eventTypes = completionEventTypes, endExclusive = endExclusive)
        endExclusive = completionIdx

        if (completionEvent.isMultisigExecutionSucceeded()) {
            val innerCall = this.extractInnerMultisigCall(call)
            endExclusive = context.endExclusiveToSkipInternalEvents(innerCall, endExclusive)
        }

        return endExclusive
    }

    override fun visit(call: GenericCall.Instance, context: VisitingContext) {
        if (!context.callSucceeded) {
            visitFailedMultisigCall(call, context)
            context.logger.info("asMulti - reverted by outer parent")
            return
        }

        val completionEventTypes = context.runtime.multisigCompletionEvents()
        val completionEvent = context.eventQueue.takeFromEndOrThrow(*completionEventTypes)

        // Not visiting pending mst's
        if (!completionEvent.isMultisigExecuted()) return

        if (completionEvent.isMultisigExecutedOk()) {
            context.logger.info("asMulti: execution succeeded")

            visitSucceededMultisigCall(call, context)
        } else {
            context.logger.info("asMulti: execution failed")

            this.visitFailedMultisigCall(call, context)
        }
    }

    private fun visitFailedMultisigCall(call: GenericCall.Instance, context: VisitingContext) {
        this.visitMultisigCall(call, context, success = false, events = emptyList())
    }

    private fun visitSucceededMultisigCall(call: GenericCall.Instance, context: VisitingContext) {
        this.visitMultisigCall(call, context, success = true, events = context.eventQueue.all())
    }

    private fun visitMultisigCall(
        call: GenericCall.Instance,
        context: VisitingContext,
        success: Boolean,
        events: List<GenericEvent.Instance>
    ) {
        val innerOrigin = extractMultisigOrigin(call, context.origin.intoKey())
        val innerCall = extractInnerMultisigCall(call)

        val visit = NestedExtrinsicVisit(
            rootExtrinsic = context.rootExtrinsic,
            call = innerCall,
            success = success,
            events = events,
            origin = innerOrigin.value
        )

        context.nestedVisit(visit)
    }

    private fun GenericEvent.Instance.isMultisigExecutionSucceeded(): Boolean {
        if (!isMultisigExecuted()) {
            // not final execution
            return false
        }

        return isMultisigExecutedOk()
    }

    private fun GenericEvent.Instance.isMultisigExecuted(): Boolean {
        return instanceOf(Modules.MULTISIG, "MultisigExecuted")
    }

    private fun GenericEvent.Instance.isMultisigExecutedOk(): Boolean {
        // dispatch_result in https://github.com/paritytech/polkadot-sdk/blob/fdf3d65e4c2d9094915c7fd7927e651339171edd/substrate/frame/multisig/src/lib.rs#L260
        return arguments[4].castToDictEnum().name == "Ok"
    }

    private fun extractInnerMultisigCall(multisigCall: GenericCall.Instance): GenericCall.Instance {
        return bindGenericCall(multisigCall.arguments["call"])
    }

    private fun RuntimeSnapshot.multisigCompletionEvents(): Array<Event> {
        val multisig = metadata.multisig()

        return arrayOf(
            multisig.event("MultisigExecuted"),
            multisig.event("MultisigApproval"),
            multisig.event("NewMultisig"),
        )
    }

    private fun extractMultisigOrigin(call: GenericCall.Instance, parentOrigin: AccountIdKey): AccountIdKey {
        val threshold = bindInt(call.arguments["threshold"])
        val otherSignatories = bindList(call.arguments["other_signatories"], ::bindAccountIdKey)

        return generateMultisigAddress(
            otherSignatories = otherSignatories,
            signatory = parentOrigin,
            threshold = threshold
        )
    }
}
