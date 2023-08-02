package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool.NominationPoolYourPoolInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponentState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NominationPoolsYourPoolComponentFactory(
    private val interactor: NominationPoolYourPoolInteractor,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
) {

    fun create(stakingOption: StakingOption, hostContext: ComponentHostContext): NominationPoolsYourPoolComponent {
        return NominationPoolsYourPoolComponent(
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            interactor = interactor,
            hostContext = hostContext,
            stakingOption = stakingOption
        )
    }
}

class NominationPoolsYourPoolComponent(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val interactor: NominationPoolYourPoolInteractor,
    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
) : YourPoolComponent, CoroutineScope by hostContext.scope {

    override val events: LiveData<Event<YourPoolEvent>> = MutableLiveData()

    override val state = nominationPoolSharedComputation.loadPoolMemberState(
        hostContext = hostContext,
        chain = stakingOption.assetWithChain.chain,
        stateProducer = ::createState,
    ).shareInBackground()

    override fun onAction(action: YourPoolAction) {
        when (action) {
            YourPoolAction.PoolInfoClicked -> handlePoolInfoClicked()
        }
    }

    private fun createState(poolMember: PoolMember): Flow<YourPoolComponentState> {
        return emptyFlow()
    }

    private fun handlePoolInfoClicked() = launch {
        val poolAccount = state.first()?.dataOrNull?.poolAccount ?: return@launch
        val chain = stakingOption.assetWithChain.chain

        hostContext.externalActions.showAddressActions(poolAccount.address, chain)
    }
}
