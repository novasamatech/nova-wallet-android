package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull

typealias QueryableStorageKeyFromInternalBinder<K> = (keyInstance: Any) -> K
typealias QueryableStorageKeyToInternalBinder<K> = (key: K) -> Any?
typealias QueryableStorageKeyBinder2<K1, K2> = (keyInstance: Any) -> Pair<K1, K2>

interface QueryableModule {

    val module: Module
}

context(RuntimeContext)
fun <T : Any> QueryableModule.storage0(name: String, binding: QueryableStorageBinder0<T>): QueryableStorageEntry0<T> {
    return RealQueryableStorageEntry0(module.storage(name), binding, this@RuntimeContext)
}

context(RuntimeContext)
fun <T : Any> QueryableModule.storage0OrNull(name: String, binding: QueryableStorageBinder0<T>): QueryableStorageEntry0<T>? {
    return module.storageOrNull(name)?.let { RealQueryableStorageEntry0(it, binding, this@RuntimeContext) }
}

context(RuntimeContext)
fun <I, T> QueryableModule.storage1(
    name: String,
    binding: QueryableStorageBinder1<I, T>,
    keyBinding: QueryableStorageKeyFromInternalBinder<I>? = null
): QueryableStorageEntry1<I, T> {
    return RealQueryableStorageEntry1(module.storage(name), binding, this@RuntimeContext, keyBinding)
}

context(RuntimeContext)
fun <I, T> QueryableModule.storage1OrNull(
    name: String,
    binding: QueryableStorageBinder1<I, T>,
    keyBinding: QueryableStorageKeyFromInternalBinder<I>? = null
): QueryableStorageEntry1<I, T>? {
    return module.storageOrNull(name)?.let { RealQueryableStorageEntry1(it, binding, this@RuntimeContext, keyBinding) }
}

context(RuntimeContext)
fun <I1, I2, T : Any> QueryableModule.storage2(
    name: String,
    binding: QueryableStorageBinder2<I1, I2, T>,
    key1ToInternalConverter: QueryableStorageKeyToInternalBinder<I1>? = null,
    key2ToInternalConverter: QueryableStorageKeyToInternalBinder<I2>? = null,
    key1FromInternalConverter: QueryableStorageKeyFromInternalBinder<I1>? = null,
    key2FromInternalConverter: QueryableStorageKeyFromInternalBinder<I2>? = null,
): QueryableStorageEntry2<I1, I2, T> {
    return RealQueryableStorageEntry2(
        storageEntry = module.storage(name),
        binding = binding,

        key1ToInternalConverter = key1ToInternalConverter,
        key2ToInternalConverter = key2ToInternalConverter,
        key1FromInternalConverter = key1FromInternalConverter,
        key2FromInternalConverter = key2FromInternalConverter
    )
}
