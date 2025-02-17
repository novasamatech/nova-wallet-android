package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.collatorStaking
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorRewardPercentage
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.extraReward
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.minStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.call.callCatching
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.RuntimeModule_ProvideMultiChainRuntimeCallsApiFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

interface MythosStakingRepository {

    fun minStakeFlow(chainId: ChainId): Flow<Balance>

    suspend fun minStake(chainId: ChainId): Balance

    suspend fun maxCollatorsPerDelegator(chainId: ChainId): Int

    suspend fun maxDelegatorsPerCollator(chainId: ChainId): Int

    suspend fun unstakeDurationInBlocks(chainId: ChainId): BlockNumber

    suspend fun maxReleaseRequests(chainId: ChainId): Int

    suspend fun perBlockReward(chainId: ChainId): Balance

    suspend fun collatorCommission(chainId: ChainId): Fraction

    suspend fun getMainStakingPot(chainId: ChainId): Result<AccountIdKey>
}

@FeatureScope
class RealMythosStakingRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageDataSource: StorageDataSource,

    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val chainRegistry: ChainRegistry
) : MythosStakingRepository {

    override fun minStakeFlow(chainId: ChainId): Flow<Balance> {
        return localStorageDataSource.subscribe(chainId) {
            metadata.collatorStaking.minStake.observeNonNull()
        }
    }

    override suspend fun minStake(chainId: ChainId): Balance {
        return localStorageDataSource.query(chainId) {
            metadata.collatorStaking.minStake.queryNonNull()
        }
    }

    override suspend fun maxCollatorsPerDelegator(chainId: ChainId): Int {
        return chainRegistry.withRuntime(chainId) {
            metadata.collatorStaking().numberConstant("MaxStakedCandidates").toInt()
        }
    }

    override suspend fun maxDelegatorsPerCollator(chainId: ChainId): Int {
        return chainRegistry.withRuntime(chainId) {
            metadata.collatorStaking().numberConstant("MaxStakers").toInt()
        }
    }

    override suspend fun unstakeDurationInBlocks(chainId: ChainId): BlockNumber {
        return chainRegistry.withRuntime(chainId) {
            metadata.collatorStaking().numberConstant("StakeUnlockDelay")
        }
    }

    override suspend fun maxReleaseRequests(chainId: ChainId): Int {
        return maxCollatorsPerDelegator(chainId)
    }

    override suspend fun perBlockReward(chainId: ChainId): Balance {
        return localStorageDataSource.query(chainId) {
            metadata.collatorStaking.extraReward.queryNonNull()
        }
    }

    override suspend fun collatorCommission(chainId: ChainId): Fraction {
        return localStorageDataSource.query(chainId) {
            metadata.collatorStaking.collatorRewardPercentage.queryNonNull()
        }
    }

    override suspend fun getMainStakingPot(chainId: ChainId): Result<AccountIdKey> {
        return multiChainRuntimeCallsApi.forChain(chainId).mainStakingPot()
    }

    private suspend fun RuntimeCallsApi.mainStakingPot(): Result<AccountIdKey> {
        return callCatching(
            section = "CollatorStakingApi",
            method = "main_pot_account",
            arguments = emptyMap(),
            returnBinding = ::bindAccountIdKey
        )
    }
}
