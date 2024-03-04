package io.novafoundation.nova.feature_account_impl.data.signer.watchOnly

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.LeafSigner
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

class WatchOnlySignerFactory(
    private val watchOnlySigningPresenter: WatchOnlyMissingKeysPresenter,
) {

    fun create(metaAccount: MetaAccount): WatchOnlySigner {
        return WatchOnlySigner(watchOnlySigningPresenter, metaAccount)
    }
}

class WatchOnlySigner(
    private val watchOnlySigningPresenter: WatchOnlyMissingKeysPresenter,
    metaAccount: MetaAccount
) : LeafSigner(metaAccount) {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        cannotSign()
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        cannotSign()
    }

    private suspend fun cannotSign(): Nothing {
        watchOnlySigningPresenter.presentNoKeysFound()

        throw SigningCancelledException()
    }
}
