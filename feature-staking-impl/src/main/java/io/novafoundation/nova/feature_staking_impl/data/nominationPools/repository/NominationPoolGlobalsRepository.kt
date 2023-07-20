package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.lastPoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.minJoinBond
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

interface NominationPoolGlobalsRepository {

    fun lastPoolId(chainId: ChainId): Flow<PoolId>

    fun observeMinJoinBond(chainId: ChainId): Flow<Balance>
}

class RealNominationPoolGlobalsRepository(
    private val localStorageDataSource: StorageDataSource,
) : NominationPoolGlobalsRepository {

    override fun lastPoolId(chainId: ChainId): Flow<PoolId> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.nominationPools.lastPoolId.observeNonNull()
                .map(::PoolId)
        }
    }

    override fun observeMinJoinBond(chainId: ChainId): Flow<Balance> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.nominationPools.minJoinBond
                .observe()
                .filterNotNull()
        }
    }
}
