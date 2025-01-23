package io.novafoundation.nova.feature_dapp_api.data.model

class DApp(
    val name: String,
    val description: String,
    val iconLink: String?,
    val url: String,
    val isFavourite: Boolean,
    val favoriteIndex: Int?
)
