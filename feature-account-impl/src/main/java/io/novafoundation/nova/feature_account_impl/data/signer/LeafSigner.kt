package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

abstract class LeafSigner(
    private val metaAccount: MetaAccount,
): NovaSigner {

    override suspend fun signerAccountId(chain: Chain): AccountId {
        return metaAccount.requireAccountIdIn(chain)
    }
}
