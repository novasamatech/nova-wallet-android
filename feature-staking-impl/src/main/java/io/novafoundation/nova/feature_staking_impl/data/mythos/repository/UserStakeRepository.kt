package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.userStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

interface UserStakeRepository {

    context(ComputationalScope)
    fun userStakeOrDefaultFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo>
}

@FeatureScope
class RealUserStakeRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageDataSource: StorageDataSource,
) : UserStakeRepository {

    context(ComputationalScope)
    override fun userStakeOrDefaultFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo> {
        return localStorageDataSource.subscribe(chainId, applyStorageDefault = true) {
            // "applyStorageDefault" applies default value defined in runtime so we should never experience null here
            metadata.collatorStaking.userStake.observeNonNull(accountId)
        }
    }
}
