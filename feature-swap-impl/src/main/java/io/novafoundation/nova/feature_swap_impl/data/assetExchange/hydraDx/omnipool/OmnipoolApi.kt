@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.utils.omnipoolOrNull
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.bindOmnipoolAssetState
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class OmnipoolApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.omnipoolOrNull: OmnipoolApi?
    get() = omnipoolOrNull()?.let(::OmnipoolApi)

context(StorageQueryContext)
val OmnipoolApi.assets: QueryableStorageEntry1<BigInteger, Unit>
    get() = storage1(
        name = "Assets",
        binding = { decoded, _ -> bindOmnipoolAssetState(decoded) },
    )




