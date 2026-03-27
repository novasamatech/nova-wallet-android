package io.novafoundation.nova.feature_assets.data.repository.defaultTokens

import io.novafoundation.nova.feature_assets.data.network.defaultTokens.DefaultAssetsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class DefaultTokensRepository(
    private val defaultAssetsApi: DefaultAssetsApi,
    private val remoteUrl: String
) {

    @Volatile
    private var cachedDefaults: Set<FullChainAssetId>? = null

    suspend fun getDefaultAssets(): Set<FullChainAssetId>? {
        cachedDefaults?.let { return it }

        val fetched = runCatching {
            val remoteAssets = defaultAssetsApi.getDefaultAssets(remoteUrl)
            remoteAssets.map { FullChainAssetId(it.chainId, it.assetId) }.toSet()
        }.getOrNull()

        val result = fetched ?: HARDCODED_DEFAULTS
        cachedDefaults = result
        return result
    }

    companion object {
        private val HARDCODED_DEFAULTS = setOf(
            FullChainAssetId("68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f", 0),  // DOT on PAH
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 1),  // DOT on Hydration
            FullChainAssetId("48239ef607d7928874027a43a67689209727dfb3d3dc5e5b03a39bdc2eda771a", 0),  // KSM on KAH
            FullChainAssetId("68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f", 1),  // USDT on PAH
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 9),  // USDT on Hydration
            FullChainAssetId("68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f", 2),  // USDC on PAH
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 15), // USDC on Hydration
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 65), // HOLLAR on Hydration
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 0),  // HDX on Hydration
            FullChainAssetId("68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f", 24), // ETH on PAH
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 57), // ETH on Hydration
            FullChainAssetId("eip155:1", 0),                                                           // ETH on Ethereum
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 50), // tBTC on Hydration
            FullChainAssetId("afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d", 48), // SOL on Hydration
        )
    }
}
