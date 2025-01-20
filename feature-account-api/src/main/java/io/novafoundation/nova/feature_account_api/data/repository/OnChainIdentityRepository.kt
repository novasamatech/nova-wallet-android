package io.novafoundation.nova.feature_account_api.data.repository

import io.novafoundation.nova.feature_account_api.data.model.AccountAddressMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

interface OnChainIdentityRepository {

    @Deprecated("Use getIdentitiesFromIds instead to avoid extra from/to hex conversions")
    suspend fun getIdentitiesFromIdsHex(chainId: ChainId, accountIdsHex: Collection<String>): AccountIdMap<OnChainIdentity?>

    suspend fun getIdentitiesFromIds(accountIds: Collection<AccountId>, chainId: ChainId): AccountIdKeyMap<OnChainIdentity?>

    suspend fun getIdentityFromId(chainId: ChainId, accountId: AccountId): OnChainIdentity?

    @Deprecated("Use getIdentitiesFromIds instead to avoid extra from/to address conversions")
    suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<OnChainIdentity?>
}
