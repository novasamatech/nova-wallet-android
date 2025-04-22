package io.novafoundation.nova.feature_account_impl.data.signer.watchOnly

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.LeafSigner
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import javax.inject.Inject

@FeatureScope
class WatchOnlySignerFactory @Inject constructor(
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

    override suspend fun signInheritedImplication(
        inheritedImplication: InheritedImplication,
        accountId: AccountId
    ): SignatureWrapper {
        cannotSign()
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        cannotSign()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return null
    }

    private suspend fun cannotSign(): Nothing {
        watchOnlySigningPresenter.presentNoKeysFound()

        throw SigningCancelledException()
    }
}
