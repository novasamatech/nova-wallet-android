package io.novafoundation.nova.feature_banners_api.di

import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory

interface BannersFeatureApi {

    fun sourceFactory(): BannersSourceFactory

    fun mixinFactory(): PromotionBannersMixinFactory
}
