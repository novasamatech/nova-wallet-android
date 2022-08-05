package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenRequester
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.awaitConfirmation
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.notSupported.ParitySignerSigningNotSupportedPresentable
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ParitySignerSigner(
    private val signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    private val signFlowRequester: ParitySignerSignInterScreenRequester,
    private val messageSigningNotSupported: ParitySignerSigningNotSupportedPresentable
) : Signer {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignatureWrapper {
        signingSharedState.set(payloadExtrinsic)

        val result = withContext(Dispatchers.Main) {
            try {
                signFlowRequester.awaitConfirmation(createNewRequest())
            } finally {
                signingSharedState.reset()
            }
        }

        if (result is ParitySignerSignInterScreenCommunicator.Response.Signed) {
            return SignatureWrapper.Sr25519(result.signature)
        } else {
            throw SigningCancelledException()
        }
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignatureWrapper {
        messageSigningNotSupported.presentSigningNotSupported()

        throw SigningCancelledException()
    }

    private fun createNewRequest(): ParitySignerSignInterScreenCommunicator.Request {
        val id = UUID.randomUUID().toString()

        return ParitySignerSignInterScreenCommunicator.Request(id)
    }
}
