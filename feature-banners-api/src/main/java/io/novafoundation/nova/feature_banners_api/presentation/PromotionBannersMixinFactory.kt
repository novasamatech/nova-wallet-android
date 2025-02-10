package io.novafoundation.nova.feature_banners_api.presentation

import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource

interface PromotionBannersMixinFactory {
    fun create(source: BannersSource): PromotionBannersMixin
}
