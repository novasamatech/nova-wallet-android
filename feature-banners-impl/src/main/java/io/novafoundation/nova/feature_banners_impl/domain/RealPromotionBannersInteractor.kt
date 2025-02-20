package io.novafoundation.nova.feature_banners_impl.domain

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.feature_banners_impl.data.BannersRepository
import kotlinx.coroutines.flow.Flow

interface PromotionBannersInteractor {

    fun observeBanners(url: String): Flow<List<PromotionBanner>>

    fun isBannerClosed(id: String): Boolean

    fun closeBanner(id: String)
}

class RealPromotionBannersInteractor(
    private val bannersRepository: BannersRepository,
) : PromotionBannersInteractor {

    override fun observeBanners(url: String): Flow<List<PromotionBanner>> {
        return flowOfAll { bannersRepository.observeBanners(url) }
    }

    override fun isBannerClosed(id: String): Boolean {
        return bannersRepository.isBannerClosed(id)
    }

    override fun closeBanner(id: String) {
        bannersRepository.closeBanner(id)
    }
}
