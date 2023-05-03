package io.novafoundation.nova.feature_external_sign_api.domain.sign.evm

import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage

interface EvmTypedMessageParser {

    fun parseEvmTypedMessage(message: String): EvmTypedMessage
}
