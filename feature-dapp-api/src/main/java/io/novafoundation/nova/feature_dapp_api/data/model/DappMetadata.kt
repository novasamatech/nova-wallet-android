package io.novafoundation.nova.feature_dapp_api.data.model

class DappCatalog(
    val categories: List<DappCategory>,
    val dApps: List<DappMetadata>
)

class DappMetadata(
    val name: String,
    val iconLink: String,
    val url: String,
    val baseUrl: String,
    val categories: Set<DappCategory>
)

data class DappCategory(
    val name: String,
    val id: String
)
