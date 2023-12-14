package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenRequester
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignatureWrapper
import io.novafoundation.nova.feature_account_api.presenatation.sign.awaitConfirmation
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SeparateFlowSigner(
    private val signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    private val signFlowRequester: SignInterScreenRequester,
) : Signer {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        signingSharedState.set(payloadExtrinsic)

        val result = withContext(Dispatchers.Main) {
            try {
                signFlowRequester.awaitConfirmation()
            } finally {
                signingSharedState.reset()
            }
        }

        if (result is SignInterScreenCommunicator.Response.Signed) {
            return SignedExtrinsic(
                payloadExtrinsic,
                SignatureWrapper(result.signature)
            )
        } else {
            throw SigningCancelledException()
        }
    }
}
