package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api

import io.novafoundation.nova.common.utils.session
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSessionIndex
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class SessionRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.session: SessionRuntimeApi
    get() = SessionRuntimeApi(session())

context(StorageQueryContext)
val SessionRuntimeApi.currentIndex: QueryableStorageEntry0<BigInteger>
    get() = storage0("CurrentIndex", binding = ::bindSessionIndex)
