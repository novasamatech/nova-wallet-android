package io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo

interface MercuryoSignatureGenerator {

    suspend fun generateSignature(address: String, secret: String, merchantTransactionId: String): String
}
