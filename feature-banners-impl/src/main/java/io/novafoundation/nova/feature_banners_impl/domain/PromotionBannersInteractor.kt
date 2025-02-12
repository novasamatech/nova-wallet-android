package io.novafoundation.nova.feature_banners_impl.domain

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner

class PromotionBannersInteractor(
    private val preferences: Preferences,
    private val bannersRepository: io.novafoundation.nova.feature_banners_impl.data.BannersRepository,
) {
    suspend fun getBanners(url: String): List<PromotionBanner> {
        val language = preferences.getCurrentLanguage()!!
        return bannersRepository.getBanners(url, language)
            .filter { !isBannerClosed(it.id) }
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
