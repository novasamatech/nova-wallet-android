package io.novafoundation.nova.feature_banners_impl.presentation.banner.source

import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor

class RealBannersSourceFactory(
    private val bannersInteractor: PromotionBannersInteractor
) : BannersSourceFactory {

    override fun create(bannersUrl: String, localisationUrl: String): BannersSource {
        return RealBannersSource(bannersUrl, localisationUrl, bannersInteractor)
    }
}
