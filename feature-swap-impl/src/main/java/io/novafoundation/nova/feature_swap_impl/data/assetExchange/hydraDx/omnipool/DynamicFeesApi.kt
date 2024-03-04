@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.utils.dynamicFees
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.DynamicFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.bindDynamicFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class DynamicFeesApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.dynamicFeesApi: DynamicFeesApi
    get() = DynamicFeesApi(dynamicFees())

context(StorageQueryContext)
val DynamicFeesApi.assetFee: QueryableStorageEntry1<HydraDxAssetId, DynamicFee>
    get() = storage1(
        name = "AssetFee",
        binding = { decoded, _ -> bindDynamicFee(decoded) },
    )
