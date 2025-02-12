package io.novafoundation.nova.feature_banners_api.presentation

import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import kotlinx.coroutines.CoroutineScope

interface PromotionBannersMixinFactory {
    fun create(source: BannersSource): PromotionBannersMixin
}
