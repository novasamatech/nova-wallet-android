package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.poolMembers
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface NominationPoolMembersRepository {

    fun observePoolMember(chainId: ChainId, accountId: AccountId): Flow<PoolMember?>
}

class RealNominationPoolMembersRepository(
    private val localStorageSource: StorageDataSource
) : NominationPoolMembersRepository {

    override fun observePoolMember(chainId: ChainId, accountId: AccountId): Flow<PoolMember?> {
        return localStorageSource.subscribe(chainId) {
            metadata.nominationPools.poolMembers.observe(accountId)
        }
    }
}
