package io.novafoundation.nova.feature_account_api.domain.account.identity

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import jp.co.soramitsu.fearless_utils.runtime.AccountId

internal class OneOfIdentityProvider(
    private val delegates: List<IdentityProvider>
): IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? {
        return delegates.tryFindNonNull {
            it.identityFor(accountId, chainId)
        }
    }
}
