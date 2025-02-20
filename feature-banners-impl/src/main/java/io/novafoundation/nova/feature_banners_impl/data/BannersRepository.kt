package io.novafoundation.nova.feature_banners_impl.data

import coil.network.HttpException
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.utils.scopeAsync
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.core.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BannersRepository {
    suspend fun getBanners(url: String): List<PromotionBanner>

    fun closeBanner(id: String)

    fun observeClosedBannerIds(): Flow<Set<String>>
}

class RealBannersRepository(
    private val preferences: Preferences,
    private val bannersApi: BannersApi,
    private val languagesHolder: LanguagesHolder
) : BannersRepository {

    companion object {
        private const val PREFS_CLOSED_BANNERS = "closed_banners"
    }

    override suspend fun getBanners(url: String): List<PromotionBanner> {
        val language = preferences.getCurrentLanguage()!!
        val bannersDeferred = scopeAsync { bannersApi.getBanners(url) }
        val localisationDeferred = scopeAsync { getLocalisation(url, language) }

        val banners = bannersDeferred.await()
        val localisation = localisationDeferred.await()

        return mapBanners(banners, localisation)
    }

    override fun observeClosedBannerIds(): Flow<Set<String>> {
        return preferences.stringSetFlow(PREFS_CLOSED_BANNERS)
            .map { it.orEmpty() }
    }

    private fun mapBanners(
        banners: List<BannerResponse>,
        localisation: Map<String, BannerLocalisationResponse>
    ) = banners.mapNotNull {
        val localisationBanner = localisation[it.id] ?: return@mapNotNull null

        PromotionBanner(
            id = it.id,
            title = localisationBanner.title,
            details = localisationBanner.details,
            backgroundUrl = it.background,
            imageUrl = it.image,
            clipToBounds = it.clipsToBounds,
            actionLink = it.action
        )
    }

    private suspend fun getLocalisation(url: String, language: Language): Map<String, BannerLocalisationResponse> {
        try {
            val localisationUrl = BannersApi.getLocalisationLink(url, language)
            return bannersApi.getBannersLocalisation(localisationUrl)
        } catch (e: HttpException) {
            val fallbackLanguage = languagesHolder.getDefaultLanguage()
            if (e.response.code == 404 && language != fallbackLanguage) {
                val fallbackUrl = BannersApi.getLocalisationLink(url, fallbackLanguage)
                return bannersApi.getBannersLocalisation(fallbackUrl)
            }

            throw e
        }
    }

    override fun closeBanner(id: String) {
        val closedBannersId = preferences.getStringSet(PREFS_CLOSED_BANNERS)
        preferences.putStringSet(PREFS_CLOSED_BANNERS, closedBannersId + id)
    }
}
