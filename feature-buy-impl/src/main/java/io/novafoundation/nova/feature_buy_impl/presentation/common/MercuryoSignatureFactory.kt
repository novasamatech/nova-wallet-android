package io.novafoundation.nova.feature_buy_impl.presentation.common

interface MercuryoSignatureFactory {

    suspend fun createSignature(address: String, secret: String, merchantTransactionId: String): String
}
