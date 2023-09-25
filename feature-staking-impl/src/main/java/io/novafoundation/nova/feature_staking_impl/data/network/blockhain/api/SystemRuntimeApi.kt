package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api

import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class SystemRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.system: SystemRuntimeApi
    get() = SystemRuntimeApi(system())

context(StorageQueryContext)
val SystemRuntimeApi.number: QueryableStorageEntry0<BigInteger>
    get() = storage0("Number", binding = ::bindBlockNumber)
