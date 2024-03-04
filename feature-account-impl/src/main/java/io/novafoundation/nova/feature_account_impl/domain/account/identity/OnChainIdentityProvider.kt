package io.novafoundation.nova.feature_account_impl.domain.account.identity

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

class OnChainIdentityProvider(
    private val onChainIdentityRepository: OnChainIdentityRepository
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? {
        val onChainIdentity = onChainIdentityRepository.getIdentityFromId(chainId, accountId)

        return Identity(onChainIdentity)
    }

    override suspend fun identitiesFor(accountIds: Collection<AccountId>, chainId: ChainId): Map<AccountIdKey, Identity?> {
        return onChainIdentityRepository.getIdentitiesFromIds(accountIds, chainId).mapValues { (_, identity) ->
            Identity(identity)
        }
    }

    private fun Identity(onChainIdentity: OnChainIdentity?): Identity? {
        return onChainIdentity?.display?.let(::Identity)
    }
}
