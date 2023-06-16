package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

interface QueryableModule {

    val module: Module
}

context(StorageQueryContext)
fun <I, T: Any> QueryableModule.storage1(name: String, binding: QueryableStorageBinder1<I, T>): QueryableStorageEntry1<I, T> {
    return RealQueryableStorageEntry1(module.storage(name), binding)
}
