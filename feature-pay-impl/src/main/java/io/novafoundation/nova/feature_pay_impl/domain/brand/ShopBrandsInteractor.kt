package io.novafoundation.nova.feature_pay_impl.domain.brand

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_pay_api.data.ShopBrandsRepository
import io.novafoundation.nova.feature_pay_api.domain.model.RaiseBrand

interface ShopBrandsInteractor {

    suspend fun prefetchBrands()

    suspend fun loadBrands(query: String, offset: PageOffset.Loadable): Result<DataPage<RaiseBrand>>
}

class RealShopBrandsInteractor(
    private val brandsRepository: ShopBrandsRepository
) : ShopBrandsInteractor {

    override suspend fun prefetchBrands() { brandsRepository.prefetch() }

    override suspend fun loadBrands(query: String, offset: PageOffset.Loadable): Result<DataPage<RaiseBrand>> {
        return brandsRepository.getBrands(query, pageOffset = offset)
    }
}
