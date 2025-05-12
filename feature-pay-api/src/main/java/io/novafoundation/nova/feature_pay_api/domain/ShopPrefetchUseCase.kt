package io.novafoundation.nova.feature_pay_api.domain

import io.novafoundation.nova.feature_pay_api.data.ShopBrandsRepository

class ShopPrefetchUseCase(
    private val brandsRepository: ShopBrandsRepository
) {

    suspend fun invoke() {
        brandsRepository.prefetch()
    }
}
