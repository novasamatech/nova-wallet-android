package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.babe
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSlot
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class BabeRuntimeApi(override val module: Module) : QueryableModule

context(RuntimeContext)
val RuntimeMetadata.babe: BabeRuntimeApi
    get() = BabeRuntimeApi(babe())

context(RuntimeContext)
val BabeRuntimeApi.currentSlot: QueryableStorageEntry0<BigInteger>
    get() = storage0("CurrentSlot", binding = ::bindSlot)

context(RuntimeContext)
val BabeRuntimeApi.genesisSlot: QueryableStorageEntry0<BigInteger>
    get() = storage0("GenesisSlot", binding = ::bindSlot)

context(RuntimeContext)
val BabeRuntimeApi.epochIndex: QueryableStorageEntry0<BigInteger>
    get() = storage0("EpochIndex", binding = ::bindNumber)
