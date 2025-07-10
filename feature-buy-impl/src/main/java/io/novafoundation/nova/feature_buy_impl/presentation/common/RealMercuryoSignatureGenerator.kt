package io.novafoundation.nova.feature_buy_impl.presentation.common

import io.novafoundation.nova.common.utils.ip.IpReceiver
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo.MercuryoSignatureGenerator
import io.novasama.substrate_sdk_android.extensions.toHexString

class RealMercuryoSignatureGenerator(
    private val ipReceiver: IpReceiver
) : MercuryoSignatureGenerator {

    override suspend fun generateSignature(address: String, secret: String, merchantTransactionId: String): String {
        val ip = ipReceiver.get()
        val signature = "$address$secret$ip$merchantTransactionId".encodeToByteArray()
            .sha512()
            .toHexString()

        return "v2:$signature"
    }
}
