package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

interface ExtrinsicWalk {

    suspend fun walk(
        source: ExtrinsicWithEvents,
        chainId: ChainId,
        visitor: ExtrinsicVisitor
    )
}

fun interface ExtrinsicVisitor {

    fun visit(visit: ExtrinsicVisit)
}

class ExtrinsicVisit(

    /**
     * Whole extrinsic object. Useful for accessing data outside if the current visit scope, e.g. some top-level events
     */
    val rootExtrinsic: ExtrinsicWithEvents,

    /**
     * Call that is currently visiting
     */
    val call: GenericCall.Instance,

    /**
     * Whether call succeeded or not.
     * Call is considered successful when it succeeds itself as well as its outer parents succeeds
     */
    val success: Boolean,

    /**
     * All events that are related to this specific call
     */
    val events: List<GenericEvent.Instance>,

    /**
     * Origin's account id that this call has been dispatched with
     */
    val origin: AccountId,

    /**
     * Whether this visit is related to a registered node or not
     */
    val hasRegisteredNode: Boolean = false
)
