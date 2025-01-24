package io.novafoundation.nova.feature_dapp_api.data.model

typealias DAppUrl = String

class DappCatalog(
    val popular: List<DAppUrl>,
    val categories: List<DappCategory>,
    val dApps: List<DappMetadata>
)

class DappMetadata(
    val name: String,
    val iconLink: String,
    val url: DAppUrl,
    val baseUrl: String,
    val categories: Set<DappCategory>
)

data class DappCategory(
    val iconUrl: String?,
    val name: String,
    val id: String
)

private const val STAKING_CATEGORY_ID = "staking"

fun DappCategory.isStaking(): Boolean {
    return id == STAKING_CATEGORY_ID
}
