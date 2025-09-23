@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_account_api.data.conversion.assethub

import io.novafoundation.nova.common.data.network.runtime.binding.bindPair
import io.novafoundation.nova.common.utils.assetConversionOrNull
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.bindMultiLocation
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class AssetConversionApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.assetConversionOrNull: AssetConversionApi?
    get() = assetConversionOrNull()?.let(::AssetConversionApi)

context(StorageQueryContext)
val AssetConversionApi.pools: QueryableStorageEntry1<Pair<RelativeMultiLocation, RelativeMultiLocation>, Unit>
    get() = storage1(
        name = "Pools",
        binding = { _, _ -> Unit },
        keyBinding = { bindPair(it, ::bindMultiLocation, ::bindMultiLocation) }
    )
