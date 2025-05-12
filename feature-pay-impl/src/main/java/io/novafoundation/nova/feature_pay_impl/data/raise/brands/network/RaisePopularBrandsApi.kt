package io.novafoundation.nova.feature_pay_impl.data.raise.brands.network

import io.novafoundation.nova.feature_pay_impl.BuildConfig
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.PopularBrandRemote
import retrofit2.http.GET

interface RaisePopularBrandsApi {

    @GET(BuildConfig.POPULAR_BRANDS_URL)
    suspend fun getPopularBrands(): List<PopularBrandRemote>

}
