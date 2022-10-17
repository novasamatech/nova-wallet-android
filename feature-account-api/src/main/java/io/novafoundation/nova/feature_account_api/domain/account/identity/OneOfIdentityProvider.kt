package io.novafoundation.nova.feature_account_api.domain.account.identity

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class OneOfIdentityProvider(
    private val delegates: List<IdentityProvider>
) : IdentityProvider {

    override suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity? = withContext(Dispatchers.IO) {
        delegates.tryFindNonNull {
            it.identityFor(accountId, chainId)
        }
    }
}
