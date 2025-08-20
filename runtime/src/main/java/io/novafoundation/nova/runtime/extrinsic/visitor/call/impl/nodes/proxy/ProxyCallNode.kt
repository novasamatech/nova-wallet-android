package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.proxy

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.ProxyCallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.CallVisitingContext
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.NestedCallVisitNode
import io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nestedVisit
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal class ProxyCallNode : NestedCallVisitNode {

    private val proxyCalls = arrayOf("proxy", "proxyAnnounced")

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.PROXY && call.function.name in proxyCalls
    }

    override fun visit(call: GenericCall.Instance, context: CallVisitingContext) {
        context.logger.info("Visiting proxy")

        val proxyVisit = RealProxyVisit(
            call = call,
            proxied = innerOrigin(call),
            nestedCall = innerCall(call),
            callOrigin = context.origin
        )

        context.visit(proxyVisit)
        context.nestedVisit(proxyVisit.nestedCall, proxyVisit.proxied)
    }

    private fun innerOrigin(proxyCall: GenericCall.Instance): AccountIdKey {
        return bindAccountIdentifier(proxyCall.arguments["real"]).intoKey()
    }

    private fun innerCall(proxyCall: GenericCall.Instance): GenericCall.Instance {
        return bindGenericCall(proxyCall.arguments["call"])
    }

    private class RealProxyVisit(
        override val call: GenericCall.Instance,
        override val proxied: AccountIdKey,
        override val nestedCall: GenericCall.Instance,
        override val callOrigin: AccountIdKey
    ) : ProxyCallVisit
}
