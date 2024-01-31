package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.decodePercentOrThrow
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.stakingRewards
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.rpc.DockRpc
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

interface DockStakingRepository {

    suspend fun getEarlyEmission(chainId: ChainId, totalStaked: Balance, totalIssuance: Balance): BigInteger

    suspend fun getTreasuryRewardsPercentage(chainId: ChainId): Percent
}

class RealDockStakingRepository(
    private val dockRpc: DockRpc,
    private val storageDataSource: StorageDataSource,
) : DockStakingRepository {

    override suspend fun getEarlyEmission(chainId: ChainId, totalStaked: Balance, totalIssuance: Balance): BigInteger = withContext(Dispatchers.IO) {
        dockRpc.stakingRewardYearlyEmission(chainId, totalStaked, totalIssuance)
    }

    override suspend fun getTreasuryRewardsPercentage(chainId: ChainId): Percent {
        return storageDataSource.query(chainId) {
            metadata.stakingRewards().constant("TreasuryRewardsPct").decodePercentOrThrow(runtime)
        }
    }
}
