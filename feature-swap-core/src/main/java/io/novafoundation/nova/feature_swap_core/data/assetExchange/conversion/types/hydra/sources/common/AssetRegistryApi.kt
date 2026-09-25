package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common

import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.assetRegistry
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class AssetRegistryApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.assetRegistry: AssetRegistryApi
    get() = AssetRegistryApi(assetRegistry())

context(StorageQueryContext)
val AssetRegistryApi.assets: QueryableStorageEntry1<HydraDxAssetId, HydrationAssetMetadata>
    get() = storage1(name = "Assets", binding = ::bindHydrationAssetMetadata)

/** Contract address from the `AccountKey20` junction of an asset's location (lowercase hex, no 0x) - set for ERC20-bound assets such as aTokens. */
context(StorageQueryContext)
val AssetRegistryApi.assetLocations: QueryableStorageEntry1<HydraDxAssetId, String?>
    get() = storage1(name = "AssetLocations", binding = { decoded, _ -> findAccountKey20(decoded) })

private fun findAccountKey20(decoded: Any?): String? {
    return when (decoded) {
        is DictEnum.Entry<*> -> if (decoded.name == "AccountKey20") {
            (decoded.value as? Struct.Instance)?.get<ByteArray>("key")?.toHexString(withPrefix = false)
        } else {
            findAccountKey20(decoded.value)
        }

        is Struct.Instance -> decoded.mapping.values.firstNotNullOfOrNull(::findAccountKey20)
        is List<*> -> decoded.firstNotNullOfOrNull(::findAccountKey20)
        else -> null
    }
}

private fun bindHydrationAssetMetadata(
    decoded: Any,
    assetId: HydraDxAssetId
): HydrationAssetMetadata {
    val asStruct = decoded.castToStruct()

    return HydrationAssetMetadata(
        assetId = assetId,
        decimals = bindInt(asStruct["decimals"]),
        assetType = asStruct.get<DictEnum.Entry<*>>("assetType")!!.name
    )
}
