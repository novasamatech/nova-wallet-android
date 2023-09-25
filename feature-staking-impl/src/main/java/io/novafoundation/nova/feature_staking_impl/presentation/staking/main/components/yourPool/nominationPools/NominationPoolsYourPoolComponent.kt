package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool.NominationPoolYourPoolInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponentState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NominationPoolsYourPoolComponentFactory(
    private val interactor: NominationPoolYourPoolInteractor,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val resourceManager: ResourceManager,
    private val poolDisplayFormatter: PoolDisplayFormatter,
) {

    fun create(stakingOption: StakingOption, hostContext: ComponentHostContext): NominationPoolsYourPoolComponent {
        return NominationPoolsYourPoolComponent(
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            interactor = interactor,
            poolDisplayFormatter = poolDisplayFormatter,
            resourceManager = resourceManager,
            hostContext = hostContext,
            stakingOption = stakingOption,
        )
    }
}

class NominationPoolsYourPoolComponent(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val interactor: NominationPoolYourPoolInteractor,
    private val resourceManager: ResourceManager,
    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
    private val poolDisplayFormatter: PoolDisplayFormatter,
) : YourPoolComponent, CoroutineScope by hostContext.scope {

    override val events: LiveData<Event<YourPoolEvent>> = MutableLiveData()

    override val state = nominationPoolSharedComputation.loadPoolMemberState(
        hostContext = hostContext,
        chain = stakingOption.assetWithChain.chain,
        stateProducer = ::createState,
        distinctUntilChanged = { old, new -> old?.poolId == new?.poolId }
    ).shareInBackground()

    override fun onAction(action: YourPoolAction) {
        when (action) {
            YourPoolAction.PoolInfoClicked -> handlePoolInfoClicked()
        }
    }

    private fun createState(poolMember: PoolMember): Flow<YourPoolComponentState> {
        val chain = stakingOption.assetWithChain.chain

        return interactor.yourPoolFlow(poolMember.poolId, chain.id).map { yourPool ->
            YourPoolComponentState(
                poolId = yourPool.id,
                display = poolDisplayFormatter.format(yourPool, chain),
                poolStash = yourPool.stashAccountId,
                title = formatPoolTitle(yourPool.id)
            )
        }
    }

    private fun formatPoolTitle(poolId: PoolId): String {
        return resourceManager.getString(R.string.nomination_pools_your_pool_format, poolId.value.toInt())
    }

    private fun handlePoolInfoClicked() = launch {
        val poolAccount = state.first()?.dataOrNull?.poolStash ?: return@launch
        val chain = stakingOption.assetWithChain.chain

        hostContext.externalActions.showAddressActions(poolAccount, chain)
    }
}
