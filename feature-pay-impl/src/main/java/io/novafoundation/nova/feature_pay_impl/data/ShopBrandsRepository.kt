package io.novafoundation.nova.feature_pay_impl.data

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaiseBrand
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaisePopularBrand

interface ShopBrandsRepository {

    suspend fun getBrands(query: String, pageOffset: PageOffset.Loadable): Result<DataPage<RaiseBrand>>

    suspend fun getPopularBrands(): Result<List<RaisePopularBrand>>
}

