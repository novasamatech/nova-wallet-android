package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.rebag
import io.novafoundation.nova.feature_staking_impl.data.repository.BagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.bagListLocator
import io.novafoundation.nova.feature_staking_impl.domain.bagList.BagListScoreConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

interface RebagInteractor {

    suspend fun calculateFee(stakingState: StakingState.Stash): Balance

    suspend fun rebag(stakingState: StakingState.Stash): Result<String>

    fun rebagMovementFlow(stakingState: StakingState.Stash): Flow<RebagMovement>
}

class RealRebagInteractor(
    private val totalIssuanceRepository: TotalIssuanceRepository,
    private val bagListRepository: BagListRepository,
    private val assetUseCase: AssetUseCase,
    private val extrinsicService: ExtrinsicService,
): RebagInteractor {

    override suspend fun calculateFee(stakingState: StakingState.Stash): Balance {
        return extrinsicService.estimateFee(stakingState.chain) {
            rebag(stakingState.stashId)
        }
    }

    override suspend fun rebag(stakingState: StakingState.Stash): Result<String> {
        return extrinsicService.submitExtrinsicWithAnySuitableWallet(stakingState.chain, stakingState.stashId) {
            rebag(stakingState.stashId)
        }
    }

    override fun rebagMovementFlow(stakingState: StakingState.Stash): Flow<RebagMovement> {
        return flowOfAll {
            val chain = stakingState.chain
            val totalIssuance = totalIssuanceRepository.getTotalIssuance(chain.id)
            val bagListLocator = bagListRepository.bagListLocator(chain.id)
            val bagScoreConverter = BagListScoreConverter.U128(totalIssuance)

            combine(
                assetUseCase.currentAssetFlow(),
                bagListRepository.listNodeFlow(stakingState.stashId, chain.id).filterNotNull()
            ) { asset, bagListNode ->
                val from = bagListLocator.bagBoundaries(bagListNode.score).map(bagScoreConverter::balanceOf)

                val actualUserScore = bagScoreConverter.scoreOf(asset.bondedInPlanks)
                val to = bagListLocator.bagBoundaries(actualUserScore).map(bagScoreConverter::balanceOf)

                RebagMovement(from = from, to = to)
            }
        }
    }
}
