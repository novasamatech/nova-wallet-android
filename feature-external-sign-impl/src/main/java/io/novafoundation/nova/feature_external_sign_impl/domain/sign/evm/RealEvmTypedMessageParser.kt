package io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm

import io.novafoundation.nova.feature_external_sign_api.domain.sign.evm.EvmTypedMessageParser
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage
import io.novasama.substrate_sdk_android.extensions.toHexString
import org.web3j.crypto.StructuredDataEncoder

internal class RealEvmTypedMessageParser : EvmTypedMessageParser {

    override fun parseEvmTypedMessage(message: String): EvmTypedMessage {
        val encoder = StructuredDataEncoder(message)

        return EvmTypedMessage(
            data = encoder.hashStructuredData().toHexString(withPrefix = true),
            raw = message
        )
    }
}
