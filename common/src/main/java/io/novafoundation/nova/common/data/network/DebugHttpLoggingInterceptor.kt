package io.novafoundation.nova.common.data.network

import io.novafoundation.nova.common.data.storage.DebugSettings
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class DebugHttpLoggingInterceptor @Inject constructor(private val settings: DebugSettings) : Interceptor {

    private val logger = HttpLoggingInterceptor.Logger { message ->
        if (!settings.nodeLogsDisabled()) HttpLoggingInterceptor.Logger.DEFAULT.log(message)
    }

    private val delegate = HttpLoggingInterceptor(logger).setLevel(HttpLoggingInterceptor.Level.BODY)

    override fun intercept(chain: Interceptor.Chain): Response {
        return if (settings.nodeLogsDisabled()) chain.proceed(chain.request()) else delegate.intercept(chain)
    }
}
