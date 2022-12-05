package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

data class ChainExplorerRemote(
    val name: String,
    val extrinsic: String?,
    val account: String?,
    val event: String?
)
