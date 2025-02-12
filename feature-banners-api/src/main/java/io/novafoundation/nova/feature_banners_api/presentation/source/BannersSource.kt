package io.novafoundation.nova.feature_banners_api.presentation.source

import io.novafoundation.nova.feature_banners_api.BuildConfig
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import kotlinx.coroutines.flow.Flow

interface BannersSourceFactory {
    fun create(url: String): BannersSource
}

interface BannersSource {
    fun observeBanners(): Flow<List<PromotionBanner>>
}

fun BannersSourceFactory.dappsSource() = create(BuildConfig.DAPPS_BANNERS_URL)

fun BannersSourceFactory.assetsSource() = create(BuildConfig.ASSETS_BANNERS_URL)
