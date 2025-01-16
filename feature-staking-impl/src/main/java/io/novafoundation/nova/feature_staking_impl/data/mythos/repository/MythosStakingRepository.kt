package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.collatorStaking
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.minStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

interface MythosStakingRepository {

    fun minStakeFlow(chainId: ChainId): Flow<Balance>

    suspend fun unstakeDurationInSessions(chainId: ChainId): Int
}

@FeatureScope
class RealMythosStakingRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageDataSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : MythosStakingRepository {

    override fun minStakeFlow(chainId: ChainId): Flow<Balance> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.collatorStaking.minStake.observeNonNull()
        }
    }

    override suspend fun unstakeDurationInSessions(chainId: ChainId): Int {
        return chainRegistry.withRuntime(chainId) {
            metadata.collatorStaking().numberConstant("StakeUnlockDelay").toInt()
        }
    }
}
