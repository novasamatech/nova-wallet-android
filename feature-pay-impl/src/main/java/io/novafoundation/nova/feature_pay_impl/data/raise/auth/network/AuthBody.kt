package io.novafoundation.nova.feature_pay_impl.data.raise.auth.network

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseBody
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseSingleData

typealias AuthBody<ATTRS> = RaiseBody<RaiseSingleData<ATTRS>>

class FirstTimeNonceRequestAttributes(
    val method: String,
    val type: String
)

class SecondTimeNonceRequestAttributes(
    val action: String
)

class NonceResponseAttributes(
    val nonce: String,
)

class ValidateVerificationRequestAttributes(
    val action: String,
    @SerializedName("signed_nonce")
    val signedNonce: String
)

class ValidateVerificationResponseAttributes(
    @SerializedName("access_token")
    val token: String,
    @SerializedName("expires_at")
    val expiresAt: Long
)
