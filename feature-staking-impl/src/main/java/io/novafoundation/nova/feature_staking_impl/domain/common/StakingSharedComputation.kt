package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.api.ExposuresWithEraIndex
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.repository.BagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.bagListLocatorOrNull
import io.novafoundation.nova.feature_staking_impl.domain.bagList.BagListScoreConverter
import io.novafoundation.nova.feature_staking_impl.domain.minimumStake
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class ActiveEraInfo(
    val eraIndex: EraIndex,
    val exposures: AccountIdMap<Exposure>,
    val minStake: Balance,
)

class StakingSharedComputation(
    private val stakingRepository: StakingRepository,
    private val computationalCache: ComputationalCache,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val accountRepository: AccountRepository,
    private val bagListRepository: BagListRepository,
    private val totalIssuanceRepository: TotalIssuanceRepository,
    private val eraTimeCalculatorFactory: EraTimeCalculatorFactory,
) {

    fun eraCalculatorFlow(stakingOption: StakingOption, scope: CoroutineScope): Flow<EraTimeCalculator> {
        val chainId = stakingOption.assetWithChain.chain.id
        val key = "ERA_TIME_CALCULATOR:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            val activeEraFlow = activeEraFlow(chainId, scope)

            eraTimeCalculatorFactory.create(stakingOption, activeEraFlow)
        }
    }

    fun activeEraFlow(chainId: ChainId, scope: CoroutineScope): Flow<EraIndex> {
        val key = "ACTIVE_ERA:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            stakingRepository.observeActiveEraIndex(chainId)
        }
    }

    fun electedExposuresWithActiveEraFlow(chainId: ChainId, scope: CoroutineScope): Flow<ExposuresWithEraIndex> {
        val key = "ELECTED_EXPOSURES:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            activeEraFlow(chainId, scope).map { eraIndex ->
                stakingRepository.getElectedValidatorsExposure(chainId, eraIndex) to eraIndex
            }
        }
    }

    fun activeEraInfo(chainId: ChainId, scope: CoroutineScope): Flow<ActiveEraInfo> {
        val key = "MIN_STAKE:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            val minBond = stakingRepository.minimumNominatorBond(chainId)
            val bagListLocator = bagListRepository.bagListLocatorOrNull(chainId)
            val totalIssuance = totalIssuanceRepository.getTotalIssuance(chainId)
            val bagListScoreConverter = BagListScoreConverter.U128(totalIssuance)
            val maxElectingVoters = bagListRepository.maxElectingVotes(chainId)
            val bagListSize = bagListRepository.bagListSize(chainId)

            electedExposuresWithActiveEraFlow(chainId, scope).map { (exposures, activeEraIndex) ->
                val minStake = minimumStake(
                    exposures = exposures.values,
                    minimumNominatorBond = minBond,
                    bagListLocator = bagListLocator,
                    bagListScoreConverter = bagListScoreConverter,
                    bagListSize = bagListSize,
                    maxElectingVoters = maxElectingVoters
                )

                ActiveEraInfo(activeEraIndex, exposures, minStake)
            }
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
        stakingOption: StakingOption,
        scope: CoroutineScope
    ): RewardCalculator {
        val chainAsset = stakingOption.assetWithChain.asset

        val key = "REWARD_CALCULATOR:${chainAsset.chainId}:${chainAsset.id}:${stakingOption.additional.stakingType}"

        return computationalCache.useCache(key, scope) {
            rewardCalculatorFactory.create(stakingOption, scope)
        }
    }
}

suspend fun StakingSharedComputation.electedExposuresInActiveEra(
    chainId: ChainId,
    scope: CoroutineScope
): AccountIdMap<Exposure> = electedExposuresInActiveEraFlow(chainId, scope).first()

fun StakingSharedComputation.electedExposuresInActiveEraFlow(chainId: ChainId, scope: CoroutineScope): Flow<AccountIdMap<Exposure>> {
    return electedExposuresWithActiveEraFlow(chainId, scope).map { (exposures, _) -> exposures }
}

suspend fun StakingSharedComputation.minStake(
    chainId: ChainId,
    scope: CoroutineScope
): Balance = activeEraInfo(chainId, scope).first().minStake
