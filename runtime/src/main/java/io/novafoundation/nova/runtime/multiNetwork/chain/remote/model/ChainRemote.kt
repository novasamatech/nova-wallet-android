package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

data class ChainRemote(
    val chainId: String,
    val name: String,
    val assets: List<ChainAssetRemote>,
    val nodes: List<ChainNodeRemote>,
    val nodeSelectionStrategy: String?,
    val explorers: List<ChainExplorerRemote>?,
    val externalApi: ChainExternalApisRemote?,
    val icon: String?,
    val addressPrefix: Int,
    val legacyAddressPrefix: Int?,
    val types: ChainTypesInfo?,
    val options: Set<String>?,
    val parentId: String?,
    val additional: Map<String, Any?>?,
)
