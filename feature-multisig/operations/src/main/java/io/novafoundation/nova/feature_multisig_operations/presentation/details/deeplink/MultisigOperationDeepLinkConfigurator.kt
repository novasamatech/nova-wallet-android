package io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink

import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.ACTION
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.CALL_HASH_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.CHAIN_ID_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.MULTISIG_ADDRESS_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.SCREEN
import android.net.Uri
import io.novafoundation.nova.common.utils.doIf
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.SIGNATORY_ADDRESS_PARAM
import io.novafoundation.nova.runtime.ext.ChainGeneses

class MultisigOperationDeepLinkData(
    val chainId: String,
    val multisigAddress: String,
    val signatoryAddress: String,
    val callHash: String
)

interface MultisigOperationDeepLinkConfigurator : DeepLinkConfigurator<MultisigOperationDeepLinkData> {

    companion object {
        const val ACTION = "open"
        const val SCREEN = "multisigOperation"
        const val PREFIX = "/$ACTION/$SCREEN"
        const val CHAIN_ID_PARAM = "chainId"
        const val MULTISIG_ADDRESS_PARAM = "multisigAddress"
        const val SIGNATORY_ADDRESS_PARAM = "signatoryAddress"
        const val CALL_HASH_PARAM = "callHash"
    }
}

class RealMultisigOperationDeepLinkConfigurator(
    private val linkBuilderFactory: LinkBuilderFactory
) : MultisigOperationDeepLinkConfigurator {

    override fun configure(payload: MultisigOperationDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        // We not add Polkadot chain id to simplify deep link
        val appendChainIdParam = payload.chainId != ChainGeneses.POLKADOT

        return linkBuilderFactory.newLink(type)
            .setAction(ACTION)
            .setScreen(SCREEN)
            .doIf(appendChainIdParam) { addParam(CHAIN_ID_PARAM, payload.chainId) }
            .addParam(MULTISIG_ADDRESS_PARAM, payload.multisigAddress)
            .addParam(SIGNATORY_ADDRESS_PARAM, payload.signatoryAddress)
            .addParam(CALL_HASH_PARAM, payload.callHash)
            .build()
    }
}
