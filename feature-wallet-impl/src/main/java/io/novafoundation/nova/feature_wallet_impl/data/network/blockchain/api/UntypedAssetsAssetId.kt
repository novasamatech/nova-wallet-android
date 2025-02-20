package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.api

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.StatemineAssetDetails
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.bindAssetDetails
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

typealias UntypedAssetsAssetId = Any

@JvmInline
value class AssetsApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
fun RuntimeMetadata.assets(palletName: String): AssetsApi {
    return AssetsApi(module(palletName))
}

context(StorageQueryContext)
val AssetsApi.asset: QueryableStorageEntry1<UntypedAssetsAssetId, StatemineAssetDetails>
    get() = storage1("Asset", binding = { decoded, _ -> bindAssetDetails(decoded) })
