package io.novafoundation.nova.feature_pay_api.di

import io.novafoundation.nova.feature_pay_api.domain.ShopPrefetchUseCase

interface PayFeatureApi {
    val shopPrefetchUseCase: ShopPrefetchUseCase
}
