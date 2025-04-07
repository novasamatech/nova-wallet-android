package io.novafoundation.nova.feature_account_impl.data.signer.ledger

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.DefaultFeeSigner
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class LedgerFeeSignerFactory @Inject constructor() {

    fun create(metaAccount: MetaAccount, chain: Chain): LedgerFeeSigner {
        return LedgerFeeSigner(DefaultFeeSigner(metaAccount, chain))
    }
}

class LedgerFeeSigner(delegate: DefaultFeeSigner) : FeeSigner by delegate {

    companion object {

        // Ledger runs with quite severe resource restrictions so we should specifically lower the number of calls per transaction
        // Otherwise Ledger will run out of RAM when decoding such a big tx
        private const val MAX_CALLS_PER_TRANSACTION = 6
    }

    override suspend fun maxCallsPerTransaction(): Int {
        return MAX_CALLS_PER_TRANSACTION
    }
}
