package io.novafoundation.nova.feature_buy_impl.presentation.common

import io.novafoundation.nova.common.utils.ip.IpAddressReceiver
import io.novafoundation.nova.common.utils.sha512
import io.novasama.substrate_sdk_android.extensions.toHexString

class RealMercuryoSignatureFactory(
    private val ipAddressReceiver: IpAddressReceiver
) : MercuryoSignatureFactory {

    override suspend fun createSignature(address: String, secret: String, merchantTransactionId: String): String {
        val ip = ipAddressReceiver.get()
        val signature = "$address$secret$ip$merchantTransactionId".encodeToByteArray()
            .sha512()
            .toHexString()

        return "v2:$signature"
    }
}
