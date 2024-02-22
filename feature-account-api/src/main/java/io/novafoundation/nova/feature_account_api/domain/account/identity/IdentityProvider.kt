package io.novafoundation.nova.feature_account_api.domain.account.identity

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

interface IdentityProvider {

    companion object;

    /**
     * Returns, if present, an identity for the given [accountId] inside specified [chainId]
     */
    suspend fun identityFor(accountId: AccountId, chainId: ChainId): Identity?

    /**
     * Bulk version of [identityFor]. Default implementation is unoptimized and just performs N single requests to [identityFor].
     */
    suspend fun identitiesFor(accountIds: Collection<AccountId>, chainId: ChainId): Map<AccountIdKey, Identity?> {
        return accountIds.associateBy(
            keySelector = ::AccountIdKey,
            valueTransform = { identityFor(it, chainId) }
        )
    }
}

fun IdentityProvider.Companion.oneOf(vararg delegates: IdentityProvider): IdentityProvider {
    return OneOfIdentityProvider(delegates.toList())
}
