package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

data class ChainRemote(
    val chainId: String,
    val name: String,
    val color: String?,
    val assets: List<ChainAssetRemote>,
    val nodes: List<ChainNodeRemote>,
    val explorers: List<ChainExplorerRemote>?,
    val externalApi: ChainExternalApiRemote?,
    val icon: String,
    val addressPrefix: Int,
    val types: ChainTypesInfo?,
    val options: List<String>?,
    val parentId: String?,
    val additional: Map<String, Any?>?,
)
