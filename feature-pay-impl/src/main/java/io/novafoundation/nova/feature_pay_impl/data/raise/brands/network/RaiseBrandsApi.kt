package io.novafoundation.nova.feature_pay_impl.data.raise.brands.network

import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.RaiseBrandResponse
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseListBody
import retrofit2.http.GET
import retrofit2.http.Query

interface RaiseBrandsApi {

    @GET("brands")
    suspend fun getBrands(
        @Query("query") query: String,
        @Query("page[size]") pageSize: Int,
        @Query("page[number]") pageNumber: Int,
        @Query("sort[field]") sortField: String = "popularity",
        @Query("sort[order]") sortOrder: String = "DESC",
    ): RaiseListBody<RaiseBrandResponse>

    @GET("brands")
    suspend fun getBrandsByIds(
        @Query("ids") ids: String,
    ): RaiseListBody<RaiseBrandResponse>
}
