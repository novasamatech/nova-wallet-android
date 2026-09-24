package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

class DefaultAssetsRemote(
    val defaultAssets: List<DefaultAssetRemote>
)

class DefaultAssetRemote(
    val chainId: String,
    val assetId: Int
)
