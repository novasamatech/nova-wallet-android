package io.novafoundation.nova.feature_banners_api.presentation.source

import io.novafoundation.nova.feature_banners_api.BuildConfig
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import kotlinx.coroutines.flow.Flow

interface BannersSourceFactory {
    fun create(bannersUrl: String, localisationUrl: String, screenName: String = "unknown"): BannersSource
}

interface BannersSource {
    val screenName: String get() = "unknown"

    fun observeBanners(): Flow<List<PromotionBanner>>
}

fun BannersSourceFactory.forDirectory(directory: String): BannersSource {
    val baseDirectory = "${BuildConfig.BANNERS_BASE_DIRECTORY}/$directory"
    val suffix = if (BuildConfig.DEBUG) "_dev" else ""
    val bannersUrl = "$baseDirectory/banners$suffix.json"
    val localisationsUrl = "$baseDirectory/localized$suffix"

    return create(bannersUrl, localisationsUrl)
}

fun BannersSourceFactory.dappsSource() = create(BuildConfig.DAPPS_BANNERS_URL, BuildConfig.DAPPS_BANNERS_LOCALISATION_URL, "dapps")

fun BannersSourceFactory.assetsSource() = create(BuildConfig.ASSETS_BANNERS_URL, BuildConfig.ASSETS_BANNERS_LOCALISATION_URL, "assets")
