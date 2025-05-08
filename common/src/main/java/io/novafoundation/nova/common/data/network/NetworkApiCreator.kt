package io.novafoundation.nova.common.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class NetworkApiCreator(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {

    fun <T> create(
        service: Class<T>,
        customBaseUrl: String = baseUrl
    ): T {
        return create(service, customBaseUrl, okHttpClient)
    }

    fun <T> create(
        service: Class<T>,
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): T {
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(service)
    }

}
