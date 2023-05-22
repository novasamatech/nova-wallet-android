package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.SelectableAssetAdditionalData
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface GenericAssetUseCase<A> {
    fun currentAssetAndOptionFlow(): Flow<AssetAndOption<A>>

    fun currentAssetFlow(): Flow<Asset> {
        return currentAssetAndOptionFlow().map { it.asset }
    }
}

interface SelectableAssetUseCase<A : SelectableAssetAdditionalData> : GenericAssetUseCase<A> {
    suspend fun availableAssetsToSelect(): List<AssetAndOption<A>>
}

data class AssetAndOption<out A>(val asset: Asset, val option: SupportedAssetOption<A>)

typealias AssetUseCase = GenericAssetUseCase<*>

typealias SelectableAssetAndOption = AssetAndOption<SelectableAssetAdditionalData>

suspend fun AssetUseCase.getCurrentAsset() = currentAssetFlow().first()
