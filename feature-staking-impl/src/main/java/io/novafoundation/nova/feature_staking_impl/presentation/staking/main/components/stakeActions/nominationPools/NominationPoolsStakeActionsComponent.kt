package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.nominationPools

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.bondMore
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.unbond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NominationPoolsStakeActionsComponentFactory(
    private val router: NominationPoolsRouter,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val resourceManager: ResourceManager,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StakeActionsComponent = NominationPoolsStakeActionsComponent(
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        router = router
    )
}

private open class NominationPoolsStakeActionsComponent(
    nominationPoolSharedComputation: NominationPoolSharedComputation,
    stakingOption: StakingOption,
    private val router: NominationPoolsRouter,
    private val hostContext: ComponentHostContext,
    private val resourceManager: ResourceManager,
) : StakeActionsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    override val state = nominationPoolSharedComputation.loadPoolMemberState(
        hostContext = hostContext,
        chain = stakingOption.assetWithChain.chain,
        stateProducer = ::stakeActionsState
    )
        .map { it?.dataOrNull }
        .shareInBackground()

    override fun onAction(action: StakeActionsAction) {
        when (action) {
            is StakeActionsAction.ActionClicked -> {
                navigateToAction(action.action)
            }
        }
    }

    private fun navigateToAction(action: ManageStakeAction) {
        when (action.id) {
            SYSTEM_MANAGE_STAKING_BOND_MORE -> router.openSetupBondMore()
            SYSTEM_MANAGE_STAKING_UNBOND -> router.openSetupUnbond()
        }
    }

    private fun stakeActionsState(poolMember: PoolMember): Flow<StakeActionsState> {
        return flowOf {
            val availableActions = availablePoolMemberActions()

            StakeActionsState(availableActions)
        }
    }

    private fun availablePoolMemberActions(): List<ManageStakeAction> = listOf(
        ManageStakeAction.bondMore(resourceManager),
        ManageStakeAction.unbond(resourceManager)
    )
}
