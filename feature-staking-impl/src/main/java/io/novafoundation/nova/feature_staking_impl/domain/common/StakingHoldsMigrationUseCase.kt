package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold.HoldId
import javax.inject.Inject

interface StakingHoldsMigrationUseCase {

    suspend fun isStakedBalanceMigratedToHolds(): Boolean
}

@FeatureScope
class RealStakingHoldsMigrationUseCase @Inject constructor(
    private val stakingSharedState: StakingSharedState,
    private val balanceHoldsRepository: BalanceHoldsRepository
) : StakingHoldsMigrationUseCase {

    override suspend fun isStakedBalanceMigratedToHolds(): Boolean {
        val chainId = stakingSharedState.chainId()
        val stakingHoldId = HoldId("Staking", "Staking")
        return balanceHoldsRepository.chainHasHoldId(chainId, stakingHoldId)
    }
}
