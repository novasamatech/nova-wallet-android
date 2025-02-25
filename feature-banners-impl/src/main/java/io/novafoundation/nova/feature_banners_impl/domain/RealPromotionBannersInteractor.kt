package io.novafoundation.nova.feature_banners_impl.domain

import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.feature_banners_impl.data.BannersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PromotionBannersInteractor {

    suspend fun observeBanners(url: String, localisationUrl: String): Flow<List<PromotionBanner>>

    fun closeBanner(id: String)
}

class RealPromotionBannersInteractor(
    private val bannersRepository: BannersRepository,
) : PromotionBannersInteractor {

    override suspend fun observeBanners(url: String, localisationUrl: String): Flow<List<PromotionBanner>> {
        val banners = bannersRepository.getBanners(url, localisationUrl)
        return bannersRepository.observeClosedBannerIds()
            .map { closedIds ->
                banners.filter { it.id !in closedIds }
            }
    }

    override fun closeBanner(id: String) {
        bannersRepository.closeBanner(id)
    }
}
