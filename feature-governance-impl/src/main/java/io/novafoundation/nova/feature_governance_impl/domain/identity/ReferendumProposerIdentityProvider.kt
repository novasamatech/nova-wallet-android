package io.novafoundation.nova.feature_governance_impl.domain.identity

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ReferendumProposerIdentityProvider(
    private val proposerFlow: Flow<ReferendumProposer?>
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? = withContext(Dispatchers.IO) {
        val proposer = proposerFlow.first()

        val maybeName = proposer?.offChainNickname?.takeIf {
            accountId.contentEquals(proposer.accountId)
        }

        maybeName?.let(::Identity)
    }
}
