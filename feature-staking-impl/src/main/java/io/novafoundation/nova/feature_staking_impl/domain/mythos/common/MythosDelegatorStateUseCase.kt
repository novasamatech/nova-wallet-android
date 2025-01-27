@file:OptIn(ExperimentalCoroutinesApi::class)

package io.novafoundation.nova.feature_staking_impl.domain.mythos.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosLocks
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.observeMythosLocks
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.total
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model.TargetWithStakedAmount
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider.MythosCollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.stakeByCollator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosCollatorWithAmount
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

interface MythosDelegatorStateUseCase {

    context(ComputationalScope)
    fun currentDelegatorState(): Flow<MythosDelegatorState>

    context(ComputationalScope)
    suspend fun getStakedCollators(state: MythosDelegatorState): List<MythosCollatorWithAmount>
}

@FeatureScope
class RealMythosDelegatorStateUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val mythosUserStakeRepository: MythosUserStakeRepository,
    private val stakingSharedState: StakingSharedState,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val collatorProvider: MythosCollatorProvider,
) : MythosDelegatorStateUseCase {

    context(ComputationalScope)
    override fun currentDelegatorState(): Flow<MythosDelegatorState> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { selectedMetaAccount ->
            stakingSharedState.assetWithChain.flatMapLatest { (chain, chainAsset) ->
                val accountId = selectedMetaAccount.accountIdIn(chain) ?: return@flatMapLatest flowOf(MythosDelegatorState.NotStarted)

                combineToPair(
                    mythosUserStakeRepository.userStakeOrDefaultFlow(chain.id, accountId),
                    balanceLocksRepository.observeMythosLocks(selectedMetaAccount.id, chain, chainAsset)
                ).transformLatest<_, MythosDelegatorState> { (userStake, mythosLocks) ->
                    collectDelegatorStake(userStake, mythosLocks, chain.id, accountId.intoKey())
                }
            }
        }
    }

    context(ComputationalScope)
    override suspend fun getStakedCollators(state: MythosDelegatorState): List<MythosCollatorWithAmount> {
        val stakeByCollator = state.stakeByCollator()
        val stakedCollatorIds = stakeByCollator.keys
        val stakingOption = stakingSharedState.selectedOption()

        val collators = collatorProvider.getCollators(stakingOption, MythosCollatorSource.Custom(stakedCollatorIds))

        return collators.map { mythosCollator ->
            TargetWithStakedAmount(
                stake = stakeByCollator.getValue(mythosCollator.accountId),
                target = mythosCollator
            )
        }
    }

    context(FlowCollector<MythosDelegatorState>)
    suspend fun collectDelegatorStake(
        userStakeInfo: UserStakeInfo,
        mythosLocks: MythosLocks,
        chainId: ChainId,
        userAccountId: AccountIdKey,
    ) {
        when {
            mythosLocks.total.isZero -> emit(MythosDelegatorState.NotStarted)

            else -> {
                val stakedStateUpdates = mythosUserStakeRepository.userDelegationsFlow(chainId, userAccountId, userStakeInfo.candidates)
                    .map { MythosDelegatorState.Staked(userStakeInfo, it, mythosLocks) }

                emitAll(stakedStateUpdates)
            }
        }
    }
}
