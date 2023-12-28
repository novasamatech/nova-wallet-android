package io.novafoundation.nova.feature_account_impl.domain.account.identity

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalIdentityProvider(
    private val accountRepository: AccountRepository
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? = withContext(Dispatchers.IO) {
        val name = accountRepository.accountNameFor(accountId, chainId)

        name?.let(::Identity)
    }
}
