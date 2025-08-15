package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap

import io.novafoundation.nova.common.utils.stableSwap
import io.novafoundation.nova.common.utils.stableSwapOrNull
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.StableSwapPoolInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.StalbeSwapPoolPegInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.bindPoolPegInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.bindStablePoolInfo
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
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

context(StorageQueryContext)
val StableSwapApi.poolPegs: QueryableStorageEntry1<HydraDxAssetId, StalbeSwapPoolPegInfo>
    get() = storage1(
        name = "PoolPegs",
        binding = { decoded, _ -> bindPoolPegInfo(decoded) },
    )
