package io.novafoundation.nova.feature_staking_api.domain.api

import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface IdentityRepository {

    suspend fun getIdentitiesFromIds(chainId: ChainId, accountIdsHex: List<String>): AccountIdMap<Identity?>

    suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<Identity?>
}
