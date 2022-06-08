package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct

class Range<T>(
    val min: T,
    val max: T,
    val ideal: T
)

fun <T> bindRange(dynamic: Any?, innerTypeBinding: (Any?) -> T): Range<T> {
    val asStruct = dynamic.castToStruct()

    return Range(
        min = innerTypeBinding(asStruct["min"]),
        max = innerTypeBinding(asStruct["max"]),
        ideal = innerTypeBinding(asStruct["ideal"])
    )
}
