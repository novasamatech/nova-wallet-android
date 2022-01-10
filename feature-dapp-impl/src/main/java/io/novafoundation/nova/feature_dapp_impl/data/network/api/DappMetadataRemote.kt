package io.novafoundation.nova.feature_dapp_impl.data.network.api

class DappMetadataResponse(
    val categories: List<DappCategoryRemote>,
    val dapps: List<DappMetadataRemote>
)

class DappMetadataRemote(
    val name: String,
    val icon: String,
    val url: String,
    val categories: List<String>
)

class DappCategoryRemote(
    val name: String,
    val id: String
)
