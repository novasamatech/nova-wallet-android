package io.novafoundation.nova.feature_banners_api.domain

class PromotionBanner(
    val id: String,
    val title: String,
    val details: String,
    val backgroundUrl: String,
    val imageUrl: String,
    val clipToBounds: Boolean,
    val actionLink: String?,
)
