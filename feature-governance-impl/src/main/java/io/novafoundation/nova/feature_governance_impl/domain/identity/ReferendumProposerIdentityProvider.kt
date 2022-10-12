package io.novafoundation.nova.feature_governance_impl.domain.identity

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReferendumProposerIdentityProvider(
    private val proposerFlow: Flow<ReferendumProposer?>
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? {
        val proposer = proposerFlow.first()

        val maybeName = proposer?.offChainNickname?.takeIf {
            accountId.contentEquals(proposer.accountId)
        }

        return maybeName?.let(::Identity)
    }
}
