package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.data.signer.SignerPayload
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenRequester
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignatureWrapper
import io.novafoundation.nova.feature_account_api.presenatation.sign.awaitConfirmation
import io.novafoundation.nova.runtime.extrinsic.signer.SignerPayloadRawWithChain
import io.novafoundation.nova.runtime.extrinsic.signer.withoutChain
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SeparateFlowSigner(
    private val signingSharedState: SigningSharedState,
    private val signFlowRequester: SignInterScreenRequester,
    private val metaAccount: MetaAccount
) : LeafSigner(metaAccount) {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        val payload = SeparateFlowSignerState(SignerPayload.Extrinsic(payloadExtrinsic), metaAccount)

        val result = awaitConfirmation(payload)

        if (result is SignInterScreenCommunicator.Response.Signed) {
            return SignedExtrinsic(
                payloadExtrinsic,
                SignatureWrapper(result.signature)
            )
        } else {
            throw SigningCancelledException()
        }
    }

    protected suspend fun useSignRawFlowRequester(payload: SignerPayloadRawWithChain): SignedRaw {
        val state = SeparateFlowSignerState(SignerPayload.Raw(payload), metaAccount)

        val result = awaitConfirmation(state)

        if (result is SignInterScreenCommunicator.Response.Signed) {
            val signature = SignatureWrapper(result.signature)
            return SignedRaw(payload.withoutChain(), signature)
        } else {
            throw SigningCancelledException()
        }
    }

    private suspend fun awaitConfirmation(state: SeparateFlowSignerState) : SignInterScreenCommunicator.Response {
        signingSharedState.set(state)

        return withContext(Dispatchers.Main) {
            try {
                signFlowRequester.awaitConfirmation()
            } finally {
                signingSharedState.reset()
            }
        }
    }
}
