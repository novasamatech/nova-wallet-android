package io.novafoundation.nova.feature_pay_api.data

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_pay_api.domain.model.RaiseBrand
import io.novafoundation.nova.feature_pay_api.domain.model.RaisePopularBrand

interface ShopBrandsRepository {

    suspend fun prefetch()

    suspend fun getBrands(query: String, pageOffset: PageOffset.Loadable): Result<DataPage<RaiseBrand>>

    suspend fun getPopularBrands(): Result<List<RaisePopularBrand>>
}

