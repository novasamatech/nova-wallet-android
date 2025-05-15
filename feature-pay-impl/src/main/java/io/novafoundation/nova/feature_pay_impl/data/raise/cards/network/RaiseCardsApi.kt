package io.novafoundation.nova.feature_pay_impl.data.raise.cards.network

import io.novafoundation.nova.feature_pay_impl.data.raise.cards.network.model.RaiseCardsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RaiseCardsApi {

    @GET("cards")
    suspend fun getCards(
        @Query("page[size]") pageSize: Int,
        @Query("sort_by") sortedBy: String = "created_at",
        @Query("sort") sort: String = "DESC"
    ): RaiseCardsResponse
}
