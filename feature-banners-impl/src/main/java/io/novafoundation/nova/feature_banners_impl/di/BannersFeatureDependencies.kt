package io.novafoundation.nova.feature_banners_impl.di

import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory


interface BannersFeatureDependencies {

    fun sourceFactory(): BannersSourceFactory

    fun mixinFactory(): PromotionBannersMixinFactory
}
