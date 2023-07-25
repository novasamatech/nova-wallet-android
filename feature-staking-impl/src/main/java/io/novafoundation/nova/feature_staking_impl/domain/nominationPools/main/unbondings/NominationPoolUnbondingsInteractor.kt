package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings

import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.electedExposuresInActiveEra
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface NominationPoolUnbondingsInteractor {

    fun unbondingsFlow(
        poolMember: PoolMember,
        chainId: ChainId,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings>
}

class RealNominationPoolUnbondingsInteractor(
    private val nominationPoolUnbondRepository: NominationPoolUnbondRepository,
    private val stakingSharedComputation: StakingSharedComputation,
) : NominationPoolUnbondingsInteractor {

    override fun unbondingsFlow(
        poolMember: PoolMember,
        chainId: ChainId,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings> {
        return combine(
            stakingSharedComputation.activeEraFlow(chainId, sharedComputationScope),
            nominationPoolUnbondRepository.unbondingPoolsFlow(poolMember.poolId, chainId)
        ) { activeEraIndex, unbondingPools ->
        }
    }
}
