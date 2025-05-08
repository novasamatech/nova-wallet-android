package io.novafoundation.nova.feature_pay_impl.data.raise.auth.network

import io.novafoundation.nova.feature_pay_impl.BuildConfig

object RaiseEndpoints {

    val BASE_URL = if (BuildConfig.DEBUG) {
        "https://sandbox-commerce-api.raise.com/business/v2/"
    } else {
        "https://commerce-api.raise.com/business/v2/"
    }

    const val FAST_SUBMISSION_RPC_URL = "https://payproxy.data.paritytech.io/"

    object Auth {

        fun isAuthEndpoint(url: String): Boolean {
            return url.startsWith("${BASE_URL}auth") || url.startsWith("${BASE_URL}tokens")
        }

        const val AUTH_TOKENS_SUFFIX = "auth/tokens"
        const val AUTH_METHODS_SUFFIX = "auth/methods"

        val AUTH_METHODS = "$BASE_URL$AUTH_METHODS_SUFFIX"
        val AUTH_TOKENS = "$BASE_URL$AUTH_TOKENS_SUFFIX"
    }
}
