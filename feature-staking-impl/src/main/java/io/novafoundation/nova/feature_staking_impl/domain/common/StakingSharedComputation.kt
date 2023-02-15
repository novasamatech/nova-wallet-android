package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest

class StakingSharedComputation(
    private val stakingRepository: StakingRepository,
    private val computationalCache: ComputationalCache,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val accountRepository: AccountRepository,
) {

    fun electedExposuresInActiveEraFlow(chainId: ChainId, scope: CoroutineScope): Flow<AccountIdMap<Exposure>> {
        val key = "ELECTED_EXPOSURES:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            stakingRepository.electedExposuresInActiveEra(chainId)
        }
    }

    fun selectedAccountStakingStateFlow(scope: CoroutineScope, assetWithChain: ChainWithAsset): Flow<StakingState> {
        val (chain, asset) = assetWithChain
        val key = "STAKING_STATE:${assetWithChain.chain.id}:${assetWithChain.asset.id}"

        return computationalCache.useSharedFlow(key, scope) {
            accountRepository.selectedMetaAccountFlow().transformLatest { account ->
                val accountId = account.accountIdIn(chain)

                if (accountId != null) {
                    emitAll(stakingRepository.stakingStateFlow(chain, asset, accountId))
                } else {
                    emit(StakingState.NonStash(chain, asset))
                }
            }
        }
    }

    suspend fun rewardCalculator(
        chainAsset: Chain.Asset,
        scope: CoroutineScope
    ): RewardCalculator {
        val key = "REWARD_CALCULATOR:${chainAsset.chainId}:${chainAsset.id}"

        return computationalCache.useCache(key, scope) {
            rewardCalculatorFactory.create(chainAsset, scope)
        }
    }
}

suspend fun StakingSharedComputation.electedExposuresInActiveEra(
    chainId: ChainId, scope: CoroutineScope
): AccountIdMap<Exposure> = electedExposuresInActiveEraFlow(chainId, scope).first()
