package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import io.novafoundation.nova.common.utils.stableSwap
import io.novafoundation.nova.common.utils.stableSwapOrNull
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.StableSwapPoolInfo
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.bindStablePoolInfo
import io.novafoundation.nova.feature_account_api.data.network.hydration.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class StableSwapApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.stableSwapOrNull: StableSwapApi?
    get() = stableSwapOrNull()?.let(::StableSwapApi)

context(StorageQueryContext)
val RuntimeMetadata.stableSwap: StableSwapApi
    get() = StableSwapApi(stableSwap())

context(StorageQueryContext)
val StableSwapApi.pools: QueryableStorageEntry1<HydraDxAssetId, StableSwapPoolInfo>
    get() = storage1(
        name = "Pools",
        binding = ::bindStablePoolInfo,
    )
