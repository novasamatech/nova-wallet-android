package io.novafoundation.nova.feature_banners_impl.data

import io.novafoundation.nova.core.model.Language
import retrofit2.http.GET
import retrofit2.http.Url

interface BannersApi {

    companion object {
        fun getLocalisationLink(url: String, language: Language): String {
            return "$url/${language.iso639Code}.json"
        }
    }

    @GET
    suspend fun getBanners(@Url url: String): List<BannerResponse>

    @GET
    suspend fun getBannersLocalisation(@Url url: String): Map<String, BannerLocalisationResponse>
}
