package io.novafoundation.nova.feature_staking_api.domain.api

import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface IdentityRepository {

    suspend fun getIdentitiesFromIds(chainId: ChainId, accountIdsHex: Collection<String>): AccountIdMap<Identity?>

    suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<Identity?>
}

suspend fun IdentityRepository.getIdentityFromId(chainId: ChainId, accountIdHex: String): Identity? {
    return getIdentitiesFromIds(chainId, listOf(accountIdHex)).values.firstOrNull()
}
