package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

typealias QueryableStorageKeyBinder<K> = (keyInstance: Any) -> K

interface QueryableModule {

    val module: Module
}

context(StorageQueryContext)
fun <T : Any> QueryableModule.storage0(name: String, binding: QueryableStorageBinder0<T>): QueryableStorageEntry0<T> {
    return RealQueryableStorageEntry0(module.storage(name), binding)
}

context(StorageQueryContext)
fun <I, T : Any> QueryableModule.storage1(
    name: String,
    binding: QueryableStorageBinder1<I, T>,
    keyBinding: QueryableStorageKeyBinder<I>? = null
): QueryableStorageEntry1<I, T> {
    return RealQueryableStorageEntry1(module.storage(name), binding, keyBinding)
}
