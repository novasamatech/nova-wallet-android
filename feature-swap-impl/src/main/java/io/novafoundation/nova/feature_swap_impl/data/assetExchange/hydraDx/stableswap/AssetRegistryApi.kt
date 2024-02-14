package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.assetRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

@JvmInline
value class AssetRegistryApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.assetRegistry: AssetRegistryApi
    get() = AssetRegistryApi(assetRegistry())

context(StorageQueryContext)
val AssetRegistryApi.assetMetadataMap: QueryableStorageEntry1<HydraDxAssetId, Int>
    get() = storage1(
        name = "AssetMetadataMap",
        binding = { decoded, _ -> bindMetadataDecimals(decoded) },
    )

private fun bindMetadataDecimals(decoded: Any): Int {
    return bindInt(decoded.castToStruct()["decimals"])
}
