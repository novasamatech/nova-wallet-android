package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.autoCompound
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.candidateStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.releaseQueues
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.userStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythDelegation
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythReleaseRequest
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

interface MythosUserStakeRepository {

    fun userStakeOrDefaultFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo>

    suspend fun userStakeOrDefault(chainId: ChainId, accountId: AccountId): UserStakeInfo

    fun userDelegationsFlow(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Flow<Map<AccountIdKey, MythDelegation>>

    suspend fun userDelegations(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Map<AccountIdKey, MythDelegation>

    suspend fun shouldClaimRewards(
        chainId: ChainId,
        accountId: AccountIdKey
    ): Boolean

    suspend fun getpPendingRewards(
        chainId: ChainId,
        accountId: AccountIdKey
    ): Balance

    fun releaseQueuesFlow(
        chainId: ChainId,
        accountId: AccountIdKey
    ): Flow<List<MythReleaseRequest>>

    suspend fun releaseQueues(
        chainId: ChainId,
        accountId: AccountIdKey
    ): List<MythReleaseRequest>

    suspend fun getAutoCompoundPercentage(
        chainId: ChainId,
        accountId: AccountIdKey
    ): Fraction

    suspend fun lastShouldRestakeSelection(): Boolean?

    suspend fun setLastShouldRestakeSelection(shouldRestake: Boolean)
}

private const val SHOULD_RESTAKE_KEY = "RealMythosUserStakeRepository.COMPOUND_MODIFIED_KEY"

@FeatureScope
class RealMythosUserStakeRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageDataSource: StorageDataSource,
    private val callApi: MultiChainRuntimeCallsApi,
    private val preferences: Preferences,
) : MythosUserStakeRepository {

    override fun userStakeOrDefaultFlow(chainId: ChainId, accountId: AccountId): Flow<UserStakeInfo> {
        return localStorageDataSource.subscribe(chainId, applyStorageDefault = true) {
            metadata.collatorStaking.userStake.observeNonNull(accountId)
        }
    }

    override suspend fun userStakeOrDefault(chainId: ChainId, accountId: AccountId): UserStakeInfo {
        return localStorageDataSource.query(chainId, applyStorageDefault = true) {
            metadata.collatorStaking.userStake.queryNonNull(accountId)
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
                resultMap.filterNotNull().mapKeys { (keys, _) -> keys.first }
            }
        }
    }

    override suspend fun userDelegations(
        chainId: ChainId,
        userId: AccountIdKey,
        delegationIds: List<AccountIdKey>
    ): Map<AccountIdKey, MythDelegation> {
        return localStorageDataSource.query(chainId) {
            val allKeys = delegationIds.map { it to userId }

            metadata.collatorStaking.candidateStake.entries(allKeys)
                .mapKeys { (keys, _) -> keys.first }
        }
    }

    override suspend fun shouldClaimRewards(chainId: ChainId, accountId: AccountIdKey): Boolean {
        return callApi.forChain(chainId).shouldClaimPendingRewards(accountId)
    }

    override suspend fun getpPendingRewards(chainId: ChainId, accountId: AccountIdKey): Balance {
        return callApi.forChain(chainId).pendingRewards(accountId)
    }

    override fun releaseQueuesFlow(chainId: ChainId, accountId: AccountIdKey): Flow<List<MythReleaseRequest>> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.collatorStaking.releaseQueues.observe(accountId.value)
                .map { it.orEmpty() }
        }
    }

    override suspend fun releaseQueues(chainId: ChainId, accountId: AccountIdKey): List<MythReleaseRequest> {
        return localStorageDataSource.query(chainId) {
            metadata.collatorStaking.releaseQueues.query(accountId.value).orEmpty()
        }
    }

    override suspend fun getAutoCompoundPercentage(chainId: ChainId, accountId: AccountIdKey): Fraction {
        return localStorageDataSource.query(chainId, applyStorageDefault = true) {
            metadata.collatorStaking.autoCompound.queryNonNull(accountId.value)
        }
    }

    override suspend fun lastShouldRestakeSelection(): Boolean? {
        if (preferences.contains(SHOULD_RESTAKE_KEY)) {
            return preferences.getBoolean(SHOULD_RESTAKE_KEY, true)
        }

        return null
    }

    override suspend fun setLastShouldRestakeSelection(shouldRestake: Boolean) {
        preferences.putBoolean(SHOULD_RESTAKE_KEY, shouldRestake)
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

    private suspend fun RuntimeCallsApi.pendingRewards(accountId: AccountIdKey): Balance {
        return call(
            section = "CollatorStakingApi",
            method = "total_rewards",
            arguments = mapOf(
                "account" to accountId.value
            ),
            returnBinding = ::bindNumber
        )
    }
}
