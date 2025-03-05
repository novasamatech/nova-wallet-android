package io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.stakeByCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model.CurrentMythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model.MythosDelegationStatus
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model.delegationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface MythosCurrentCollatorsInteractor {

    context(ComputationalScope)
    fun currentCollatorsFlow(): Flow<GroupedList<MythosDelegationStatus, CurrentMythosCollator>>
}

@FeatureScope
class RealMythosCurrentCollatorsInteractor @Inject constructor(
    private val delegatorStateUseCase: MythosDelegatorStateUseCase,
) : MythosCurrentCollatorsInteractor {

    context(ComputationalScope)
    override fun currentCollatorsFlow(): Flow<GroupedList<MythosDelegationStatus, CurrentMythosCollator>> {
        return delegatorStateUseCase.currentDelegatorState()
            .distinctUntilChangedBy { it.stakeByCollator() }
            .mapLatest { delegatorState ->
                delegatorStateUseCase.getStakedCollators(delegatorState).map { collatorWithAmount ->
                    CurrentMythosCollator(
                        collator = collatorWithAmount.target,
                        userStake = collatorWithAmount.stake,
                        status = collatorWithAmount.target.delegationStatus()
                    )
                }.groupBy { it.status }
            }
    }
}
