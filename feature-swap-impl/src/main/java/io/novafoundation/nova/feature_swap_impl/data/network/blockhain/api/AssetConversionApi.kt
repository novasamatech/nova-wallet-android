@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_impl.data.network.blockhain.api

import io.novafoundation.nova.common.data.network.runtime.binding.bindPair
import io.novafoundation.nova.common.utils.assetConversionOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.bindMultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

@JvmInline
value class AssetConversionApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.assetConversionOrNull: AssetConversionApi?
    get() = assetConversionOrNull()?.let(::AssetConversionApi)

context(StorageQueryContext)
val AssetConversionApi.pools: QueryableStorageEntry1<Pair<MultiLocation, MultiLocation>, Unit>
    get() = storage1(
        name = "Pools",
        binding = { _, _ -> Unit },
        keyBinding = { bindPair(it, ::bindMultiLocation, ::bindMultiLocation) }
    )
