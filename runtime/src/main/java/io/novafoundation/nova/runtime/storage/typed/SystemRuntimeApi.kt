package io.novafoundation.nova.runtime.storage.typed

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.EventRecord
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindEventRecords
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class SystemRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.system: SystemRuntimeApi
    get() = SystemRuntimeApi(system())

context(StorageQueryContext)
val SystemRuntimeApi.number: QueryableStorageEntry0<BigInteger>
    get() = storage0("Number", binding = ::bindBlockNumber)

context(StorageQueryContext)
val SystemRuntimeApi.account: QueryableStorageEntry1<AccountId, AccountInfo>
    get() = storage1("Account", binding = { decoded, _ -> bindAccountInfo(decoded) })

context(StorageQueryContext)
val SystemRuntimeApi.events: QueryableStorageEntry0<List<EventRecord>>
    get() = storage0("Events", binding = ::bindEventRecords)
