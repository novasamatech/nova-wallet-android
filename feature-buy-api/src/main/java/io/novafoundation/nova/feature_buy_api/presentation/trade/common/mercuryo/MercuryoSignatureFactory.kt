package io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo

interface MercuryoSignatureFactory {

    suspend fun createSignature(address: String, secret: String, merchantTransactionId: String): String
}
