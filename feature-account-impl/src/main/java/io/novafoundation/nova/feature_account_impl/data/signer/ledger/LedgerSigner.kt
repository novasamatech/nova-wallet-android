package io.novafoundation.nova.feature_account_impl.data.signer.ledger

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.signer.SeparateFlowSigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.runtime.ext.isMigrationLedgerAppSupported
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

class LedgerSignerFactory(
    private val signingSharedState: SigningSharedState,
    private val signFlowRequester: LedgerSignCommunicator,
    private val resourceManager: ResourceManager,
    private val messageSigningNotSupported: SigningNotSupportedPresentable
) {

    fun create(metaAccount: MetaAccount, ledgerVariant: LedgerVariant): LedgerSigner {
        return LedgerSigner(
            metaAccount = metaAccount,
            signingSharedState = signingSharedState,
            signFlowRequester = signFlowRequester,
            resourceManager = resourceManager,
            messageSigningNotSupported = messageSigningNotSupported,
            ledgerVariant = ledgerVariant
        )
    }
}

class LedgerSigner(
    metaAccount: MetaAccount,
    signingSharedState: SigningSharedState,
    private val signFlowRequester: LedgerSignCommunicator,
    private val ledgerVariant: LedgerVariant,
    private val resourceManager: ResourceManager,
    private val messageSigningNotSupported: SigningNotSupportedPresentable,
) : SeparateFlowSigner(signingSharedState, signFlowRequester, metaAccount) {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        signFlowRequester.setUsedVariant(ledgerVariant)

        return super.signExtrinsic(payloadExtrinsic)
    }

    override suspend fun supportsCheckMetadataHash(chain: Chain): Boolean {
        return when (ledgerVariant) {
            LedgerVariant.LEGACY -> chain.additional.isMigrationLedgerAppSupported()

            LedgerVariant.GENERIC -> true
        }
    }

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
