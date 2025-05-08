package io.novafoundation.nova.feature_pay_impl.domain.brand

import io.novafoundation.nova.feature_pay_impl.data.raise.brands.ShopBrandsRepository

interface ShopBrandsInteractor {

    suspend operator fun invoke()
}

class RealShopBrandsInteractor(
    private val brandsRepository: ShopBrandsRepository
) : ShopBrandsInteractor {

    override suspend operator fun invoke() { brandsRepository.prefetch() }
}
