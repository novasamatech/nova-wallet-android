package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

fun bindGenericCall(decoded: Any?): GenericCall.Instance {
    return decoded.cast()
}
