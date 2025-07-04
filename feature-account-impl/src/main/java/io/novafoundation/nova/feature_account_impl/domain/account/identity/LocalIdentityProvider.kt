package io.novafoundation.nova.feature_account_impl.domain.account.identity

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalIdentityProvider(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? = withContext(Dispatchers.IO) {
        val name = accountRepository.accountNameFor(accountId, chainId)

        name?.let(::Identity)
    }

    override suspend fun identitiesFor(accountIds: Collection<AccountId>, chainId: ChainId): Map<AccountIdKey, Identity?> {
        val chain = chainRegistry.getChain(chainId)
        val metaAccountsById = accountRepository.getActiveMetaAccounts()
            .associateBy { it.accountIdIn(chain)?.intoKey() }

        return accountIds.associateBy(
            keySelector = { it.intoKey() },
            valueTransform = { metaAccountsById[it.intoKey()]?.name?.let(::Identity) }
        )
    }
}
