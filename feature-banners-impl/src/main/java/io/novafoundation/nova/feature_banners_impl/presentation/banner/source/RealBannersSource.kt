package io.novafoundation.nova.feature_banners_impl.presentation.banner.source

import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor

class RealBannersSource(
    private val url: String,
    private val bannersInteractor: PromotionBannersInteractor
) : BannersSource {

    override suspend fun getBanners(): List<PromotionBanner> {
        return bannersInteractor.getBanners(url)
    }
}
