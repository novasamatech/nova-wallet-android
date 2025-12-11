package io.novafoundation.nova.feature_gift_api.di

import io.novafoundation.nova.feature_gift_api.domain.AreGiftsSupportedUseCase
import io.novafoundation.nova.feature_gift_api.domain.AvailableGiftAssetsUseCase

interface GiftFeatureApi {

    val giftDeepLinks: GiftDeepLinks

    val availableGiftAssetsUseCase: AvailableGiftAssetsUseCase

    val areGiftsSupportedUseCase: AreGiftsSupportedUseCase
}
