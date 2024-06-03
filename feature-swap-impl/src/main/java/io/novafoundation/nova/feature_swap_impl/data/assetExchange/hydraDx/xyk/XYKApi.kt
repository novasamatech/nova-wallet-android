package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.utils.xyk
import io.novafoundation.nova.common.utils.xykOrNull
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.XYKPoolInfo
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.bindXYKPoolInfo
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class XYKSwapApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.xykOrNull: XYKSwapApi?
    get() = xykOrNull()?.let(::XYKSwapApi)

context(StorageQueryContext)
val RuntimeMetadata.xyk: XYKSwapApi
    get() = XYKSwapApi(xyk())

context(StorageQueryContext)
val XYKSwapApi.poolAssets: QueryableStorageEntry1<AccountIdKey, XYKPoolInfo>
    get() = storage1(
        name = "PoolAssets",
        keyBinding = { bindAccountId(it).intoKey() },
        binding = { decoded, _ -> bindXYKPoolInfo(decoded) },
    )
