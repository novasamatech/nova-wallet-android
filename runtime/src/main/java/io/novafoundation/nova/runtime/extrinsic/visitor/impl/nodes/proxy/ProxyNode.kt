package io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.proxy

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.EventCountingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.NestedCallNode
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.VisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.peekItemFromEndOrThrow
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.takeFromEndOrThrow
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.metadata.event
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Event

internal class ProxyNode : NestedCallNode {

    private val proxyCalls = arrayOf("proxy", "proxyAnnounced")


    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.PROXY && call.function.name in proxyCalls
    }

    override fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, context: EventCountingContext): Int {
        var endExclusive = context.endExclusive
        val completionEventType = context.runtime.proxyCompletionEvent()

        val (completionEvent, completionIdx) = context.eventQueue.peekItemFromEndOrThrow(completionEventType, endExclusive = endExclusive)
        endExclusive = completionIdx

        if (completionEvent.isProxySucceeded()) {
            val (innerCall) = this.callAndOriginFromProxy(call)
            endExclusive = context.endExclusiveToSkipInternalEvents(innerCall, endExclusive)
        }

        return endExclusive
    }

    override fun visit(call: GenericCall.Instance, context: VisitingContext) {
        if (!context.callSucceeded) {
            this.visitFailedProxyCall(call, context)
            context.logger.info("Proxy: reverted by outer parent")
            return
        }

        val completionEventType = context.runtime.proxyCompletionEvent()
        val completionEvent = context.eventQueue.takeFromEndOrThrow(completionEventType)

        if (completionEvent.isProxySucceeded()) {
            context.logger.info("Proxy: execution succeeded")

            this.visitSucceededProxyCall(call, context)
        } else {
            context.logger.info("Proxy: execution failed")

            this.visitFailedProxyCall(call, context)
        }
    }

    private fun visitFailedProxyCall(call: GenericCall.Instance, context: VisitingContext) {
        this.visitProxyCall(call, context, success = false, events = emptyList())
    }

    private fun visitSucceededProxyCall(call: GenericCall.Instance, context: VisitingContext) {
        this.visitProxyCall(call, context, success = true, events = context.eventQueue.all())
    }

    private fun visitProxyCall(
        call: GenericCall.Instance,
        context: VisitingContext,
        success: Boolean,
        events: List<GenericEvent.Instance>
    ) {
        val (innerCall, innerOrigin) = this.callAndOriginFromProxy(call)

        val visit = ExtrinsicVisit(
            rootExtrinsic = context.rootExtrinsic,
            call = innerCall,
            success = success,
            events = events,
            origin = innerOrigin
        )

        context.nestedVisit(visit)
    }


    private fun GenericEvent.Instance.isProxySucceeded(): Boolean {
        return arguments.first().castToDictEnum().name == "Ok"
    }

    private fun callAndOriginFromProxy(proxyCall: GenericCall.Instance): Pair<GenericCall.Instance, AccountId> {
        return bindGenericCall(proxyCall.arguments["call"]) to bindAccountIdentifier(proxyCall.arguments["real"])
    }

    private fun RuntimeSnapshot.proxyCompletionEvent(): Event {
        return metadata.proxy().event("ProxyExecuted")
    }
}
