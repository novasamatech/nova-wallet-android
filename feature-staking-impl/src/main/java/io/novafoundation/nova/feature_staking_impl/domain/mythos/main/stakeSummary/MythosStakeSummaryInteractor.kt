package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.hasActiveValidators
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.participatingBondedPoolStateFlow
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.poolState.isPoolStaking
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

interface MythosStakeSummaryInteractor {

    context(ComputationalScope)
    fun stakeSummaryFlow(
        userStakeInfo: UserStakeInfo,
        stakingOption: StakingOption,
    ): Flow<StakeSummary<MythosDelegatorStatus>>
}

@FeatureScope
class RealMythosStakeSummaryInteractor @Inject constructor(
    private val mythosSharedComputation: MythosSharedComputation,
) : MythosStakeSummaryInteractor {

    context(ComputationalScope)
    override fun stakeSummaryFlow(
        userStakeInfo: UserStakeInfo,
        stakingOption: StakingOption,
    ): Flow<StakeSummary<MythosDelegatorStatus>> {
        val chainId = stakingOption.assetWithChain.chain.id

        return mythosSharedComputation.sessionValidatorsFlow(chainId).map { sessionValidators ->
            val status = when {
                userStakeInfo.hasActiveValidators(sessionValidators) -> MythosDelegatorStatus.Active
                else -> MythosDelegatorStatus.Inactive
            }

            StakeSummary(
                status = status,
                activeStake = userStakeInfo.balance
            )
        }
    }
}
