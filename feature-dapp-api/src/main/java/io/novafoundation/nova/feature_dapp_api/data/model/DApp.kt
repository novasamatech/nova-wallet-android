package io.novafoundation.nova.feature_dapp_api.data.model

import io.novafoundation.nova.common.list.GroupedList

class DApp(
    val name: String,
    val description: String,
    val iconLink: String?,
    val url: String,
    val isFavourite: Boolean,
    val favoriteIndex: Int?
)

data class DAppGroupedCatalog(
    val popular: List<DApp>,
    val categoriesWithDApps: GroupedList<DappCategory, DApp>
)
