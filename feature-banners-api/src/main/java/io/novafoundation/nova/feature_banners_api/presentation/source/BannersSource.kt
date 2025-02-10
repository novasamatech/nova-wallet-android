package io.novafoundation.nova.feature_banners_api.presentation.source

import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner

interface BannersSourceFactory {
    fun create(url: String): BannersSource
}

interface BannersSource {
    suspend fun getBanners(): List<PromotionBanner>
}
