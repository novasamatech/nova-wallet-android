package io.novafoundation.nova.feature_banners_impl.data

class BannerResponse(
    val id: String,
    val background: String,
    val image: String,
    val clipsToBounds: Boolean,
    val action: String?
)

class BannerLocalisationResponse(
    val title: String,
    val details: String
)
