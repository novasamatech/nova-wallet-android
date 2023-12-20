package io.novafoundation.nova.feature_account_impl.data.signer.ledger

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenRequester
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.signer.SeparateFlowSigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class LedgerSignerFactory(
    private val signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    private val signFlowRequester: SignInterScreenRequester,
    private val resourceManager: ResourceManager,
    private val messageSigningNotSupported: SigningNotSupportedPresentable
) {

    fun create(metaAccount: MetaAccount): LedgerSigner {
        return LedgerSigner(
            metaAccount = metaAccount,
            signingSharedState = signingSharedState,
            signFlowRequester = signFlowRequester,
            resourceManager = resourceManager,
            messageSigningNotSupported = messageSigningNotSupported
        )
    }
}

class LedgerSigner(
    metaAccount: MetaAccount,
    signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    signFlowRequester: SignInterScreenRequester,
    private val resourceManager: ResourceManager,
    private val messageSigningNotSupported: SigningNotSupportedPresentable
) : SeparateFlowSigner(signingSharedState, signFlowRequester, metaAccount) {

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        messageSigningNotSupported.presentSigningNotSupported(
            SigningNotSupportedPresentable.Payload(
                iconRes = R.drawable.ic_ledger,
                message = resourceManager.getString(R.string.ledger_sign_raw_not_supported)
            )
        )

        throw SigningCancelledException()
    }
}
