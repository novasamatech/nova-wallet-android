package io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink

import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.ACTION
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.CALL_HASH_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.CHAIN_ID_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.MULTISIG_ADDRESS_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.SCREEN
import android.net.Uri
import io.novafoundation.nova.common.utils.doIf
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilder
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.addParamIfNotNull
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.ACTOR_IDENTITY_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.CALL_DATA_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.OPERATION_STATE_PARAM
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator.Companion.SIGNATORY_ADDRESS_PARAM
import io.novafoundation.nova.runtime.ext.ChainGeneses

class MultisigOperationDeepLinkData(
    val chainId: String,
    val multisigAddress: String,
    val signatoryAddress: String,
    val callHash: String,
    val callData: String?,
    val operationState: State?,
) {
    sealed interface State {
        companion object;

        data object Active : State
        class Rejected(val actorIdentity: String) : State
        class Executed(val actorIdentity: String) : State
    }
}

interface MultisigOperationDeepLinkConfigurator : DeepLinkConfigurator<MultisigOperationDeepLinkData> {

    companion object {
        const val ACTION = "open"
        const val SCREEN = "multisigOperation"
        const val PREFIX = "/$ACTION/$SCREEN"
        const val CHAIN_ID_PARAM = "chainId"
        const val MULTISIG_ADDRESS_PARAM = "multisigAddress"
        const val SIGNATORY_ADDRESS_PARAM = "signatoryAddress"
        const val ACTOR_IDENTITY_PARAM = "actorIdentity"
        const val CALL_HASH_PARAM = "callHash"
        const val CALL_DATA_PARAM = "callData"
        const val OPERATION_STATE_PARAM = "operationState"
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
            .addState(payload.operationState)
            .addParam(MULTISIG_ADDRESS_PARAM, payload.multisigAddress)
            .addParam(SIGNATORY_ADDRESS_PARAM, payload.signatoryAddress)
            .addParam(CALL_HASH_PARAM, payload.callHash)
            .addParamIfNotNull(CALL_DATA_PARAM, payload.callData)
            .build()
    }
}

fun Uri.getOperationState(): MultisigOperationDeepLinkData.State? {
    return MultisigOperationDeepLinkData.State.fromString(getQueryParameter(OPERATION_STATE_PARAM), this)
}

private fun LinkBuilder.addState(state: MultisigOperationDeepLinkData.State?): LinkBuilder {
    return addParamIfNotNull(OPERATION_STATE_PARAM, state?.mapToString())
        .addParamIfNotNull(ACTOR_IDENTITY_PARAM, state?.actorIdentityOrNull())
}

private fun MultisigOperationDeepLinkData.State.mapToString() = when (this) {
    MultisigOperationDeepLinkData.State.Active -> "active"
    is MultisigOperationDeepLinkData.State.Executed -> "executed"
    is MultisigOperationDeepLinkData.State.Rejected -> "rejected"
}

private fun MultisigOperationDeepLinkData.State.Companion.fromString(value: String?, uri: Uri) = when (value) {
    "active" -> MultisigOperationDeepLinkData.State.Active
    "executed" -> MultisigOperationDeepLinkData.State.Executed(uri.getQueryParameter(ACTOR_IDENTITY_PARAM)!!)
    "rejected" -> MultisigOperationDeepLinkData.State.Rejected(uri.getQueryParameter(ACTOR_IDENTITY_PARAM)!!)
    else -> null
}

fun MultisigOperationDeepLinkData.State.actorIdentityOrNull() = when (this) {
    MultisigOperationDeepLinkData.State.Active -> null
    is MultisigOperationDeepLinkData.State.Executed -> actorIdentity
    is MultisigOperationDeepLinkData.State.Rejected -> actorIdentity
}
