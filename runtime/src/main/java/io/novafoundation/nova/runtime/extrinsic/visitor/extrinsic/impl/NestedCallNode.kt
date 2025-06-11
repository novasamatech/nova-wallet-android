package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl

import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisitor
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

internal interface NestedCallNode {

    fun canVisit(call: GenericCall.Instance): Boolean

    /**
     * Calculates exclusive end index that is needed to skip all internal events related to this nested call
     * For example, utility.batch supposed to skip BatchCompleted/BatchInterrupted and all ItemCompleted events
     * This function is used by `visit` to skip internal events of nested nodes of the same type (batch inside batch or proxy inside proxy)
     * so they wont interfere
     * Should not be called on failed nested calls since they emit no events and its trivial to proceed
     */
    fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, context: EventCountingContext): Int

    fun visit(call: GenericCall.Instance, context: VisitingContext)
}

internal interface VisitingContext {

    val rootExtrinsic: ExtrinsicWithEvents

    val runtime: RuntimeSnapshot

    val origin: AccountId

    val callSucceeded: Boolean

    val visitor: ExtrinsicVisitor

    val logger: ExtrinsicVisitorLogger

    val eventQueue: MutableEventQueue

    fun nestedVisit(visit: NestedExtrinsicVisit)

    fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance): Int
}

/**
 * Version of [ExtrinsicVisit] intended for nested usage
 *
 * @see [ExtrinsicVisit]
 */
internal class NestedExtrinsicVisit(
    val rootExtrinsic: ExtrinsicWithEvents,
    val call: GenericCall.Instance,
    val success: Boolean,
    val events: List<GenericEvent.Instance>,
    val origin: AccountId,
)

internal interface EventCountingContext {

    val runtime: RuntimeSnapshot

    val eventQueue: EventQueue

    val endExclusive: Int

    fun endExclusiveToSkipInternalEvents(call: GenericCall.Instance, endExclusive: Int): Int
}
