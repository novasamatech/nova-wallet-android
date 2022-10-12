package io.novafoundation.nova.feature_account_impl.domain.account.identity

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class LocalIdentityProvider(
    private val accountRepository: AccountRepository
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? {
        val name = accountRepository.accountNameFor(accountId)

        return name?.let(::Identity)
    }
}
