package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.nominationPools

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary.NominationPoolStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary.PoolMemberStatus
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.BaseStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest

class NominationPoolsStakeSummaryComponentFactory(
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val interactor: NominationPoolStakeSummaryInteractor,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): StakeSummaryComponent = NominationPoolsStakeSummaryComponent(
        stakingOption = stakingOption,
        hostContext = hostContext,
        poolMemberUseCase = poolMemberUseCase,
        interactor = interactor
    )
}

private class NominationPoolsStakeSummaryComponent(
    poolMemberUseCase: NominationPoolMemberUseCase,
    private val interactor: NominationPoolStakeSummaryInteractor,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : BaseStakeSummaryComponent(hostContext.scope) {

    override val state: Flow<StakeSummaryState?> = poolMemberUseCase.loadPoolMemberState(
        hostContext = hostContext,
        stateProducer = ::poolMemberStakeSummary
    )
        .shareInBackground()

    private fun poolMemberStakeSummary(poolMember: PoolMember): Flow<StakeSummaryModel> {
        return interactor.stakeSummaryFlow(poolMember, stakingOption, sharedComputationScope = this).flatMapLatest { stakeSummary ->
            val status = mapPoolMemberStatusToUi(stakeSummary.status)

            hostContext.assetFlow.mapLatest { asset ->
                StakeSummaryModel(
                    totalStaked = mapAmountToAmountModel(stakeSummary.totalStaked, asset),
                    status = status
                )
            }
        }
    }

    private fun mapPoolMemberStatusToUi(
        poolMemberStatus: PoolMemberStatus
    ): StakeStatusModel {
        return when (poolMemberStatus) {
            PoolMemberStatus.Active -> StakeStatusModel.Active()
            PoolMemberStatus.Inactive -> StakeStatusModel.Inactive()
            is PoolMemberStatus.Waiting -> StakeStatusModel.Waiting(
                timeLeft = poolMemberStatus.timeLeft.inWholeMilliseconds,
                messageFormat = R.string.staking_nominator_status_waiting_format,
            )
        }
    }
}
