package io.novafoundation.nova.feature_account_impl.data.signer.ledger

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.signer.SeparateFlowSigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import javax.inject.Inject

@FeatureScope
class LedgerSignerFactory @Inject constructor(
    private val signingSharedState: SigningSharedState,
    private val signFlowRequester: LedgerSignCommunicator,
    private val resourceManager: ResourceManager,
    private val messageSigningNotSupported: SigningNotSupportedPresentable,
) {

    fun create(metaAccount: MetaAccount, ledgerVariant: LedgerVariant): LedgerSigner {
        return LedgerSigner(
            metaAccount = metaAccount,
            signingSharedState = signingSharedState,
            signFlowRequester = signFlowRequester,
            resourceManager = resourceManager,
            messageSigningNotSupported = messageSigningNotSupported,
            ledgerVariant = ledgerVariant,
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

    companion object {

        // Ledger runs with quite severe resource restrictions so we should explicitly lower the number of calls per transaction
        // Otherwise Ledger will run out of RAM when decoding such big txs
        private const val MAX_CALLS_PER_TRANSACTION = 6
    }

    override suspend fun signInheritedImplication(inheritedImplication: InheritedImplication, accountId: AccountId): SignatureWrapper {
        signFlowRequester.setUsedVariant(ledgerVariant)

        return super.signInheritedImplication(inheritedImplication, accountId)
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

    override suspend fun maxCallsPerTransaction(): Int {
        return MAX_CALLS_PER_TRANSACTION
    }
}
