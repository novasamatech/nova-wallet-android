package io.novafoundation.nova.runtime.extrinsic.visitor.call.api

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface CallTraversal {

    fun traverse(
        source: GenericCall.Instance,
        initialOrigin: AccountIdKey,
        visitor: CallVisitor
    )
}

fun interface CallVisitor {

    fun visit(visit: CallVisit)
}

fun CallTraversal.collect(
    source: GenericCall.Instance,
    initialOrigin: AccountIdKey,
): List<CallVisit> {
    return buildList {
        traverse(source, initialOrigin) {
            add(it)
        }
    }
}
