package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

fun bindGenericCall(decoded: Any?): GenericCall.Instance {
    return decoded.cast()
}

fun bindGenericCallList(decoded: Any?): List<GenericCall.Instance> {
    return bindList(decoded, ::bindGenericCall)
}
