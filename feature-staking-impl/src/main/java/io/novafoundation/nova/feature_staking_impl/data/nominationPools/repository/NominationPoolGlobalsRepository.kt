package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.datasource.KnownMaxUnlockingOverwrites
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.lastPoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.minJoinBond
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.math.BigInteger

interface NominationPoolGlobalsRepository {

    fun lastPoolIdFlow(chainId: ChainId): Flow<PoolId>

    suspend fun lastPoolId(chainId: ChainId): PoolId

    suspend fun maxUnlockChunks(chainId: ChainId): BigInteger

    fun observeMinJoinBond(chainId: ChainId): Flow<Balance>
}

class RealNominationPoolGlobalsRepository(
    private val localStorageDataSource: StorageDataSource,
    private val knownMaxUnlockingOverwrites: KnownMaxUnlockingOverwrites,
    private val stakingRepository: StakingConstantsRepository,
) : NominationPoolGlobalsRepository {

    override fun lastPoolIdFlow(chainId: ChainId): Flow<PoolId> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.nominationPools.lastPoolId.observeNonNull()
                .map(::PoolId)
        }
    }

    override suspend fun lastPoolId(chainId: ChainId): PoolId {
        return localStorageDataSource.query(chainId) {
            val poolIdRaw = metadata.nominationPools.lastPoolId.query()

            PoolId(poolIdRaw!!)
        }
    }

    override suspend fun maxUnlockChunks(chainId: ChainId): BigInteger {
        val overwrite = knownMaxUnlockingOverwrites.getUnlockChunksFor(chainId)
        if (overwrite != null) return overwrite

        return stakingRepository.maxUnlockingChunks(chainId)
    }

    override fun observeMinJoinBond(chainId: ChainId): Flow<Balance> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.nominationPools.minJoinBond
                .observe()
                .filterNotNull()
        }
    }
}
