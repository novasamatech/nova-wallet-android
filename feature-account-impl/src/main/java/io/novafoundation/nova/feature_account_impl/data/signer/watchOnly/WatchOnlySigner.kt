package io.novafoundation.nova.feature_account_impl.data.signer.watchOnly

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.sign.WatchOnlySigningPresenter
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class WatchOnlySigner(
    private val watchOnlySigningPresenter: WatchOnlySigningPresenter
) : Signer {

    override suspend fun signRaw(payload: SignerPayloadRaw): SignatureWrapper {
        watchOnlySigningPresenter.presentSigningNotPossible()

        throw SigningCancelledException()
    }
}
