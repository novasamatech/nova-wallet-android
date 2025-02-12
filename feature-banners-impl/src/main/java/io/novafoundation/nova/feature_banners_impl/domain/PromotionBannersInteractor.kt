package io.novafoundation.nova.feature_banners_impl.domain

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.feature_banners_impl.data.BannersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PromotionBannersInteractor(
    private val preferences: Preferences,
    private val bannersRepository: BannersRepository,
) {
    fun observeBanners(url: String): Flow<List<PromotionBanner>> {
        val language = preferences.getCurrentLanguage()!!
        return flowOfAll {
            val banners = bannersRepository.getBanners(url, language)

            val keys = banners.map { getBannerPreferenceKey(it.id) }.toTypedArray()
            preferences.keysFlow(*keys)
                .map { banners.filter { !isBannerClosed(it.id) } }
        }
    }

    fun isBannerClosed(id: String): Boolean {

        return preferences.getBoolean(getBannerPreferenceKey(id), false)
    }

    fun closeBanner(id: String) {
        preferences.putBoolean(getBannerPreferenceKey(id), true)
    }

    private fun getBannerPreferenceKey(id: String): String {
        return "closed_banner_$id"
    }
}
