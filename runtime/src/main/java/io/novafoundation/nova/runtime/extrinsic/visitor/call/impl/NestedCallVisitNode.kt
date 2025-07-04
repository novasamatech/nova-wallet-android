package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.runtime.extrinsic.visitor.ExtrinsicVisitorLogger
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal interface NestedCallVisitNode {

    fun canVisit(call: GenericCall.Instance): Boolean

    fun visit(call: GenericCall.Instance, context: CallVisitingContext)
}

internal interface CallVisitingContext {

    val origin: AccountIdKey

    val logger: ExtrinsicVisitorLogger

    /**
     * Request parent to perform recursive visit of the given call
     */
    fun nestedVisit(visit: NestedCallVisit)

    /**
     * Call the supplied visitor with the given argument
     */
    fun visit(visit: CallVisit)
}

internal fun CallVisitingContext.nestedVisit(call: GenericCall.Instance, origin: AccountIdKey) {
    nestedVisit(NestedCallVisit(call, origin))
}

/**
 * Version of [CallVisit] intended for nested usage
 *
 * @see [CallVisit]
 */
internal class NestedCallVisit(
    val call: GenericCall.Instance,

    val origin: AccountIdKey
)
