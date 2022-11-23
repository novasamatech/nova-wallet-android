package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.GenericSingleAssetSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface AssetUseCase {

    fun currentAssetAndOptionFlow(): Flow<AssetAndOption>

    fun currentAssetFlow(): Flow<Asset> {
        return currentAssetAndOptionFlow().map { it.asset }
    }

    suspend fun availableAssetsToSelect(): List<AssetAndOption>
}

suspend fun AssetUseCase.getCurrentAsset() = currentAssetFlow().first()

data class AssetAndOption(val asset: Asset, val option: GenericSingleAssetSharedState.SupportedAssetOption<*>)
