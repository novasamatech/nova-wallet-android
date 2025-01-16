package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import io.novasama.substrate_sdk_android.runtime.metadata.storage

typealias QueryableStorageKeyBinder<K> = (keyInstance: Any) -> K
typealias QueryableStorageKeyBinder2<K1, K2> = (keyInstance: Any) -> Pair<K1, K2>

interface QueryableModule {

    val module: Module
}

context(RuntimeContext)
fun <T : Any> QueryableModule.storage0(name: String, binding: QueryableStorageBinder0<T>): QueryableStorageEntry0<T> {
    return RealQueryableStorageEntry0(module.storage(name), binding, this@RuntimeContext)
}

context(RuntimeContext)
fun <I, T> QueryableModule.storage1(
    name: String,
    binding: QueryableStorageBinder1<I, T>,
    keyBinding: QueryableStorageKeyBinder<I>? = null
): QueryableStorageEntry1<I, T> {
    return RealQueryableStorageEntry1(module.storage(name), binding, this@RuntimeContext, keyBinding)
}

context(RuntimeContext)
fun <I1, I2, T : Any> QueryableModule.storage2(
    name: String,
    binding: QueryableStorageBinder2<I1, I2, T>,
): QueryableStorageEntry2<I1, I2, T> {
    return RealQueryableStorageEntry2(module.storage(name), binding)
}
