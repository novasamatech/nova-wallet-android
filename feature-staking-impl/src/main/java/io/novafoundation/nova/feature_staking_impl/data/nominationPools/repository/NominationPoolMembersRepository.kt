package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.poolMembers
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface NominationPoolMembersRepository {

    fun observePoolMember(chainId: ChainId, accountId: AccountId): Flow<PoolMember?>

    suspend fun getPendingRewards(poolMemberAccountId: AccountId, chainId: ChainId): Balance
}

class RealNominationPoolMembersRepository(
    private val localStorageSource: StorageDataSource,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
) : NominationPoolMembersRepository {

    override fun observePoolMember(chainId: ChainId, accountId: AccountId): Flow<PoolMember?> {
        return localStorageSource.subscribe(chainId) {
            metadata.nominationPools.poolMembers.observe(accountId)
        }
    }

    override suspend fun getPendingRewards(poolMemberAccountId: AccountId, chainId: ChainId): Balance {
        return multiChainRuntimeCallsApi.forChain(chainId).call(
            section = "NominationPoolsApi",
            method = "pending_rewards",
            arguments = listOf(poolMemberAccountId to "GenericAccountId"),
            returnType = "Balance",
            returnBinding = ::bindNumber
        )
    }
}
