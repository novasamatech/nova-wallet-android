package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.candidateStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.userStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythDelegation
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

interface UserStakeRepository {

    fun userStakeFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo?>

    fun userDelegationsFlow(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Flow<Map<AccountIdKey, MythDelegation>>
}

@FeatureScope
class RealUserStakeRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageDataSource: StorageDataSource,
) : UserStakeRepository {

    override fun userStakeFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo?> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.collatorStaking.userStake.observe(accountId)
        }
    }

    override fun userDelegationsFlow(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Flow<Map<AccountIdKey, MythDelegation>> {
        return localStorageDataSource.subscribe(chainId) {
            val allKeys = delegationIds.map { userId to it }

            metadata.collatorStaking.candidateStake.observe(allKeys).map { resultMap ->
                resultMap.mapKeys { (keys, _) -> keys.first }
                    .filterNotNull()
            }
        }
    }

}
