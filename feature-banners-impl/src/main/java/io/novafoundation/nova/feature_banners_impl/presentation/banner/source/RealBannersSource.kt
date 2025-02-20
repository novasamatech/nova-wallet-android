package io.novafoundation.nova.feature_banners_impl.presentation.banner.source

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor
import kotlinx.coroutines.flow.Flow

class RealBannersSource(
    private val url: String,
    private val bannersInteractor: PromotionBannersInteractor
) : BannersSource {

    override fun observeBanners(): Flow<List<PromotionBanner>> {
        return flowOfAll { bannersInteractor.observeBanners(url) }
    }
}
