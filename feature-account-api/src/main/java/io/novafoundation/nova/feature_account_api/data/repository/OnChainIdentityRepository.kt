package io.novafoundation.nova.feature_account_api.data.repository

import io.novafoundation.nova.feature_account_api.data.model.AccountAddressMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface OnChainIdentityRepository {

    suspend fun getIdentitiesFromIds(chainId: ChainId, accountIdsHex: Collection<String>): AccountIdMap<OnChainIdentity?>

    suspend fun getIdentityFromId(chainId: ChainId, accountId: AccountId): OnChainIdentity?

    suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<OnChainIdentity?>
}

suspend fun OnChainIdentityRepository.getIdentityFromId(chainId: ChainId, accountIdHex: String): OnChainIdentity? {
    return getIdentitiesFromIds(chainId, listOf(accountIdHex)).values.firstOrNull()
}
