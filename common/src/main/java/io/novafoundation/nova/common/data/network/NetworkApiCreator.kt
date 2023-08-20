package io.novafoundation.nova.common.data.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class NetworkApiCreator(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val baseUrl: String
) {

    fun <T> create(
        service: Class<T>,
        customBaseUrl: String = baseUrl
    ): T {
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(customBaseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(service)
    }
}
