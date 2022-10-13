package io.novafoundation.nova.feature_account_impl.domain.account.identity

import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class OnChainIdentityProvider(
    private val onChainIdentityRepository: OnChainIdentityRepository
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? {
        val onChainIdentity = onChainIdentityRepository.getIdentityFromId(chainId, accountId)
        val name = onChainIdentity?.display

        return name?.let(::Identity)
    }
}
