package io.novafoundation.nova.feature_external_sign_api.di

import io.novafoundation.nova.feature_external_sign_api.domain.sign.evm.EvmTypedMessageParser

interface ExternalSignFeatureApi {

    val evmTypedMessageParser: EvmTypedMessageParser
}
