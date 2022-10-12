package io.novafoundation.nova.feature_account_api.domain.account.identity

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface IdentityProvider {

    companion object;

    suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity?
}

fun IdentityProvider.Companion.oneOf(vararg delegates: IdentityProvider): IdentityProvider {
    return OneOfIdentityProvider(delegates.toList())
}
