package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.candidateStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.releaseQueues
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.userStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythDelegation
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythReleaseRequest
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

interface MythosUserStakeRepository {

    fun userStakeOrDefaultFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo>

    fun userDelegationsFlow(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Flow<Map<AccountIdKey, MythDelegation>>

    suspend fun shouldClaimRewards(
        chainId: ChainId,
        accountId: AccountIdKey
    ): Boolean

    fun releaseQueuesFlow(
        chainId: ChainId,
        accountId: AccountIdKey
    ): Flow<List<MythReleaseRequest>>
}

@FeatureScope
class RealMythosUserStakeRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageDataSource: StorageDataSource,
    private val callApi: MultiChainRuntimeCallsApi,
) : MythosUserStakeRepository {

    override fun userStakeOrDefaultFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo> {
        return localStorageDataSource.subscribe(chainId, applyStorageDefault = true) {
            metadata.collatorStaking.userStake.observeNonNull(accountId)
        }
    }

    override fun userDelegationsFlow(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Flow<Map<AccountIdKey, MythDelegation>> {
        return localStorageDataSource.subscribe(chainId) {
            val allKeys = delegationIds.map { it to userId }

            metadata.collatorStaking.candidateStake.observe(allKeys).map { resultMap ->
                resultMap.mapKeys { (keys, _) -> keys.first }
                    .filterNotNull()
            }
        }
    }

    override suspend fun shouldClaimRewards(chainId: ChainId, accountId: AccountIdKey): Boolean {
        return callApi.forChain(chainId).shouldClaimPendingRewards(accountId)
    }

    override fun releaseQueuesFlow(chainId: ChainId, accountId: AccountIdKey): Flow<List<MythReleaseRequest>> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.collatorStaking.releaseQueues.observe(accountId.value)
                .map { it.orEmpty() }
        }
    }

    private suspend fun RuntimeCallsApi.shouldClaimPendingRewards(accountId: AccountIdKey): Boolean {
        return call(
            section = "CollatorStakingApi",
            method = "should_claim",
            arguments = mapOf(
                "account" to accountId.value
            ),
            returnBinding = ::bindBoolean
        )
    }
}
