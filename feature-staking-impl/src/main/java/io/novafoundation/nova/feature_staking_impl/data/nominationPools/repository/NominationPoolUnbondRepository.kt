package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.subPoolsStorage
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import kotlinx.coroutines.flow.Flow

interface NominationPoolUnbondRepository {

    fun unbondingPoolsFlow(poolId: PoolId, chainId: ChainId): Flow<UnbondingPools?>
}

class RealNominationPoolUnbondRepository(
    private val localStorageDataSource: StorageDataSource,
) : NominationPoolUnbondRepository {

    override fun unbondingPoolsFlow(poolId: PoolId, chainId: ChainId): Flow<UnbondingPools?> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.nominationPools.subPoolsStorage.observe(poolId.value)
        }
    }
}
