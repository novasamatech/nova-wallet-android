package io.novafoundation.nova.feature_banners_impl.data

import retrofit2.http.GET
import retrofit2.http.Url

interface BannersApi {

    @GET
    suspend fun getBanners(@Url url: String): List<BannerResponse>

    @GET
    suspend fun getBannersLocalisation(@Url url: String): Map<String, BannerLocalisationResponse>
}
