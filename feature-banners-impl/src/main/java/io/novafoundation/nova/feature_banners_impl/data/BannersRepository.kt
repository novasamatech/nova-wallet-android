package io.novafoundation.nova.feature_banners_impl.data

import coil.network.HttpException
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.utils.asyncWithContext
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.core.model.Language

interface BannersRepository {
    suspend fun getBanners(url: String, language: Language): List<PromotionBanner>
}

class RealBannersRepository(
    private val bannersApi: BannersApi,
    private val languagesHolder: LanguagesHolder
) : BannersRepository {

    override suspend fun getBanners(url: String, language: Language): List<PromotionBanner> {
        val bannersDeferred = asyncWithContext { bannersApi.getBanners(url) }
        val localisationDeferred = asyncWithContext { getLocalisation(url, language) }

        val banners = bannersDeferred.await()
        val localisation = localisationDeferred.await()

        return banners.mapNotNull {
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
    }

    private suspend fun getLocalisation(url: String, language: Language): Map<String, BannerLocalisationResponse> {
        try {
            val localisationUrl = getLocalisationLink(url, language)
            return bannersApi.getBannersLocalisation(localisationUrl)
        } catch (e: HttpException) {
            val fallbackLanguage = languagesHolder.getDefaultLanguage()
            if (e.response.code == 404 && language != fallbackLanguage) {
                val fallbackUrl = getLocalisationLink(url, fallbackLanguage)
                return bannersApi.getBannersLocalisation(fallbackUrl)
            }

            throw e
        }
    }

    private fun getLocalisationLink(url: String, language: Language): String {
        val baseBannersUrl = url.substringBeforeLast("/")
        return "$baseBannersUrl/localized/${language.iso639Code}.json"
    }
}
