package io.novafoundation.nova.feature_account_impl.data.signer.ledger

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenRequester
import io.novafoundation.nova.feature_account_impl.data.signer.SeparateFlowSigner
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.notSupported.ParitySignerSigningNotSupportedPresentable
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class LedgerSigner(
    signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    signFlowRequester: SignInterScreenRequester,
    private val messageSigningNotSupported: ParitySignerSigningNotSupportedPresentable
) : SeparateFlowSigner(signingSharedState, signFlowRequester) {

    override suspend fun signRaw(payload: SignerPayloadRaw): SignatureWrapper {
        messageSigningNotSupported.presentSigningNotSupported()

        throw SigningCancelledException()
    }
}
