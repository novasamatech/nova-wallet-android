package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService.DetectionState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform

interface StakingStartedDetectionService {

    enum class DetectionState {
        ACTIVE, PAUSED
    }

    fun observeStatingStarted(stakingOptionIds: MultiStakingOptionIds, screenScope: CoroutineScope): Flow<Chain>

    suspend fun setDetectionState(state: DetectionState, screenScope: CoroutineScope)
}

suspend fun StakingStartedDetectionService.activateDetection(screenScope: CoroutineScope) {
    setDetectionState(DetectionState.ACTIVE, screenScope)
}

suspend fun StakingStartedDetectionService.pauseDetection(screenScope: CoroutineScope) {
    setDetectionState(DetectionState.PAUSED, screenScope)
}

suspend fun StakingStartedDetectionService.awaitStakingStarted(stakingOptionIds: MultiStakingOptionIds, screenScope: CoroutineScope): Chain {
    return observeStatingStarted(stakingOptionIds, screenScope).first()
}

private const val CACHE_KEY = "RealStakingStartedDetectionService.detectionActiveState"

class RealStakingStartedDetectionService(
    private val stakingDashboardRepository: StakingDashboardRepository,
    private val computationalCache: ComputationalCache,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) : StakingStartedDetectionService {

    override fun observeStatingStarted(
        stakingOptionIds: MultiStakingOptionIds,
        screenScope: CoroutineScope
    ): Flow<Chain> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
            stakingDashboardRepository.dashboardItemsFlow(account.id, stakingOptionIds)
                .transform { dashboardItems ->
                    val hasAnyStake = dashboardItems.any { item -> item.stakeState is StakingDashboardItem.StakeState.HasStake }

                    if (hasAnyStake) {
                        val chain = chainRegistry.getChain(stakingOptionIds.chainId)

                        awaitDetectionActive(screenScope)

                        emit(chain)
                    }
                }
        }
    }

    override suspend fun setDetectionState(state: DetectionState, screenScope: CoroutineScope) {
        detectionActiveState(screenScope).value = state
    }

    private suspend fun awaitDetectionActive(screenScope: CoroutineScope) {
        detectionActiveState(screenScope).first { it == DetectionState.ACTIVE }
    }

    private suspend fun detectionActiveState(scope: CoroutineScope): MutableStateFlow<DetectionState> {
        return computationalCache.useCache(CACHE_KEY, scope) {
            MutableStateFlow(DetectionState.ACTIVE)
        }
    }
}
