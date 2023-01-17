package io.novafoundation.nova.feature_dapp_impl.data.network.metadata

class DappMetadataResponse(
    val categories: List<DappCategoryRemote>,
    val dapps: List<DappMetadataRemote>
)

class DappMetadataRemote(
    val name: String,
    val icon: String,
    val url: String,
    val categories: List<String>,
    val desktopOnly: Boolean?
)

class DappCategoryRemote(
    val name: String,
    val id: String
)
