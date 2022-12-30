package io.novafoundation.nova.feature_dapp_api.data.model

class DappMetadata(
    val name: String,
    val iconLink: String,
    val url: String,
    val baseUrl: String,
    val categories: Set<DappCategory>,
    val desktopOnly: Boolean
)

data class DappCategory(
    val name: String,
    val id: String
)
