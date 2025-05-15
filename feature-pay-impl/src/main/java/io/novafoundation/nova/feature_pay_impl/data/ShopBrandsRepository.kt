package io.novafoundation.nova.feature_pay_impl.data

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrand
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopPopularBrand

interface ShopBrandsRepository {

    suspend fun getBrands(query: String, pageOffset: PageOffset.Loadable): Result<DataPage<ShopBrand>>

    suspend fun getPopularBrands(): Result<List<ShopPopularBrand>>
}
