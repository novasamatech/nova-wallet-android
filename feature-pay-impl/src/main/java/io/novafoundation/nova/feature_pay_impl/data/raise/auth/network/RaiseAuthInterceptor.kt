package io.novafoundation.nova.feature_pay_impl.data.raise.auth.network

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_pay_impl.BuildConfig
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RaiseAuthToken
import io.novafoundation.nova.feature_pay_impl.data.raise.common.network.RaiseBody
import io.novafoundation.nova.feature_pay_impl.data.raise.common.network.RaiseSingleData
import io.novafoundation.nova.feature_pay_impl.data.raise.common.network.RaiseSingleObjectBody
import io.novafoundation.nova.runtime.ext.polkadot
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.util.fromJson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class RaiseAuthInterceptor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val raiseAuthRepository: RaiseAuthRepository,
    private val gson: Gson,
) : Interceptor {

    // Not in constructor to break circular dependency between Interceptor and OkOkHttpClient
    lateinit var client: OkHttpClient

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()

        if (RaiseEndpoints.Auth.isAuthEndpoint(request.url.toString())) return chain.proceed(request)

        val builder: Request.Builder = request.newBuilder()
        builder.header("Accept", "application/json")

        val selectedMetaAccount = runBlocking { accountRepository.getSelectedMetaAccount() }

        val token = synchronized(client) {
            var token = raiseAuthRepository.getJwtToken(selectedMetaAccount)
            if (token == null || token.hasExpired()) {
                refreshToken(selectedMetaAccount, token)
                    .onSuccess {
                        token = it
                    }.onFailure {
                        Log.e(LOG_TAG, "Failed to refresh Raise token")

                        return chain.proceed(request)
                    }
            }
            token
        }

        setAuthHeader(builder, token!!)

        request = builder.build()

        val response: Response = chain.proceed(request)
        val code = response.code

        if (code == 401 || code == 403) {
            val currentToken = synchronized(client) {
                // perform all 401 in sync blocks, to avoid multiply token updates
                var currentToken = raiseAuthRepository.getJwtToken(selectedMetaAccount)

                // compare current token with token that was stored before, if it was not updated - do update
                if (currentToken == token) {
                    refreshToken(selectedMetaAccount, currentToken)
                        .onSuccess {
                            currentToken = it
                        }.onFailure {
                            Log.e(LOG_TAG, "Failed to refresh Raise token")

                            return response
                        }
                }
                currentToken
            }
            setAuthHeader(builder, currentToken!!)
            request = builder.build()
            return chain.proceed(request)
        }

        return response
    }

    private fun setAuthHeader(builder: Request.Builder, token: RaiseAuthToken) {
        builder.header("Authorization", String.format("Bearer %s", token.token))
    }

    private fun refreshToken(metaAccount: MetaAccount, currentJwtToken: RaiseAuthToken?): Result<RaiseAuthToken> {
        return runCatching {
            val publicKey = raiseAuthRepository.getChallengePublicKey(metaAccount)
            val walletAddress = runBlocking { metaAccount.requireAddressIn(chainRegistry.polkadot()) }

            val nonce = getNonce(currentJwtToken, walletAddress, publicKey)
            val signedNonce = raiseAuthRepository.signChallenge(metaAccount, nonce)
            validateVerification(signedNonce, walletAddress).also {
                raiseAuthRepository.saveJwtToken(metaAccount, it)
            }
        }
    }

    private fun getNonce(currentJwtToken: RaiseAuthToken?, customerId: String, publicKey: ByteArray): ByteArray {
        // Already existing token - second time nonce request for sue
        if (currentJwtToken != null) {
            Log.d(LOG_TAG, "Refreshing already existing JWT token")

            return getSecondTimeNonce(customerId)
        }

        return try {
            Log.d(LOG_TAG, "Attempting to get first time JWT token")

            getFirstTimeNonce(customerId, publicKey)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "First time JWT token retrieval failed, trying to refresh existing one")

            // We have failed getting first time nonce - maybe user has recovered already existing account?
            // We try second time nonce
            getSecondTimeNonce(customerId)
        }
    }

    private fun getFirstTimeNonce(customerId: String, publicKey: ByteArray): ByteArray {
        val request = Request.Builder()
            .url(RaiseEndpoints.Auth.AUTH_METHODS)
            .addHeader("X-CustomerID", customerId)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", raiseAuthRepository.basicAuthCredentials())
            .post(prepareFirstTimeNonceRequestBody(publicKey))
            .build()

        return client.newCall(request).execute().use { response ->
            parseNonceResponse(response).data.attributes.nonce.fromHex()
        }
    }

    private fun getSecondTimeNonce(customerId: String): ByteArray {
        val request = Request.Builder()
            .url(RaiseEndpoints.Auth.AUTH_TOKENS)
            .addHeader("X-CustomerID", customerId)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", raiseAuthRepository.basicAuthCredentials())
            .post(prepareSecondTimeNonceRequestBody())
            .build()

        return client.newCall(request).execute().use { response ->
            parseNonceResponse(response).data.attributes.nonce.fromHex()
        }
    }

    private fun validateVerification(signedNonce: ByteArray, customerId: String): RaiseAuthToken {
        val request = Request.Builder()
            .url(RaiseEndpoints.Auth.AUTH_TOKENS)
            .addHeader("X-CustomerID", customerId)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", raiseAuthRepository.basicAuthCredentials())
            .post(prepareValidateVerificationRequestBody(signedNonce))
            .build()

        return client.newCall(request).execute().use { response ->
            val attrs = parseValidateVerificationResponse(response).data.attributes

            RaiseAuthToken(attrs.token, attrs.expiresAt)
        }
    }

    private fun parseNonceResponse(response: Response): RaiseSingleObjectBody<NonceResponseAttributes> {
        return gson.fromJson(response.stringBodyOrThrow())
    }

    private fun prepareFirstTimeNonceRequestBody(publicKey: ByteArray): RequestBody {
        val body = RaiseBody(
            data = RaiseSingleData(
                type = RaiseEndpoints.Auth.AUTH_METHODS_SUFFIX,
                attributes = FirstTimeNonceRequestAttributes(
                    method = publicKey.toHexString(withPrefix = true),
                    type = "SR25519_KEY_PAIR"
                )
            )
        )
        val json = gson.toJson(body)

        return json.toRequestBody("application/json".toMediaType())
    }

    private fun prepareSecondTimeNonceRequestBody(): RequestBody {
        val body = RaiseBody(
            data = RaiseSingleData(
                type = RaiseEndpoints.Auth.AUTH_TOKENS_SUFFIX,
                attributes = SecondTimeNonceRequestAttributes(
                    action = "GENERATE_NONCE"
                )
            )
        )
        val json = gson.toJson(body)

        return json.toRequestBody("application/json".toMediaType())
    }

    private fun prepareValidateVerificationRequestBody(signedNonce: ByteArray): RequestBody {
        val body = RaiseBody(
            data = RaiseSingleData(
                type = RaiseEndpoints.Auth.AUTH_TOKENS_SUFFIX,
                attributes = ValidateVerificationRequestAttributes(
                    action = "VALIDATE_VERIFICATION",
                    signedNonce = signedNonce.toHexString(withPrefix = true)
                )
            )
        )
        val json = gson.toJson(body)

        return json.toRequestBody("application/json".toMediaType())
    }

    private fun parseValidateVerificationResponse(response: Response): AuthBody<ValidateVerificationResponseAttributes> {
        return gson.fromJson(response.stringBodyOrThrow())
    }

    private fun Response.stringBodyOrThrow(): String {
        return if (isSuccessful) {
            body!!.string()
        } else {
            error("Failed to fetch auth nonce ${logBody().orEmpty()}")
        }
    }

    private fun Response.logBody(): String? {
        return if (BuildConfig.DEBUG) {
            body?.string()
        } else {
            null
        }
    }
}
