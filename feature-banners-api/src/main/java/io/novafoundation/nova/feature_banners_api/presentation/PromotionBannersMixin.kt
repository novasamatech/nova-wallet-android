package io.novafoundation.nova.feature_banners_api.presentation

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import kotlinx.coroutines.flow.Flow

interface PromotionBannersMixin {

    val bannersFlow: Flow<ExtendedLoadingState<List<BannerPageModel>>>
}
