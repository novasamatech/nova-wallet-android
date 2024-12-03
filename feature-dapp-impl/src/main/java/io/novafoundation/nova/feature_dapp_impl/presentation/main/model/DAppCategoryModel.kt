package io.novafoundation.nova.feature_dapp_impl.presentation.main.model

data class DAppCategoryModel(
    val id: String,
    val iconUrl: String?,
    val name: String,
    val selected: Boolean
)

class DAppCategoryState(
    val categories: List<DAppCategoryModel>,
    val selectedIndex: Int?
)
