package io.novafoundation.nova.runtime.network.binding

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.runtime.storage.source.query.DynamicInstanceBinder
import java.math.BigInteger

fun <T> collectionOf(itemBinder: DynamicInstanceBinder<T>): DynamicInstanceBinder<List<T>> {
    return { bindList(it, itemBinder) }
}

fun number(): DynamicInstanceBinder<BigInteger> = ::bindNumber
