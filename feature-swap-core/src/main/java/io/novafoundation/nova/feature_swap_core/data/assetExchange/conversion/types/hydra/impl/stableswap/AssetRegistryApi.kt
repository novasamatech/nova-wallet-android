package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap

import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.assetRegistry
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class AssetRegistryApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.assetRegistry: AssetRegistryApi
    get() = AssetRegistryApi(assetRegistry())

context(StorageQueryContext)
val AssetRegistryApi.assetMetadataMap: QueryableStorageEntry1<HydraDxAssetId, Int?>
    get() = storage1(
        name = "Assets",
        binding = { decoded, _ -> bindMetadataDecimals(decoded) },
    )

private fun bindMetadataDecimals(decoded: Any): Int? {
    return decoded.castToStruct().get<Any?>("decimals")?.let(::bindInt)
}
