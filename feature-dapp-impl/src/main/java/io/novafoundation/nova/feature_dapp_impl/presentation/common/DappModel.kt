package io.novafoundation.nova.feature_dapp_impl.presentation.common

data class DappModel(
    val name: String,
    val description: String,
    val iconUrl: String?,
    val isFavourite: Boolean,
    val url: String
)
