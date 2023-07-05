package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.parachain

import android.util.Log
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.BaseNetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class ParachainNetworkInfoComponentFactory(
    private val interactor: ParachainNetworkInfoInteractor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val resourceManager: ResourceManager,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): NetworkInfoComponent = ParachainNetworkInfoComponent(
        interactor = interactor,
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext
    )
}

private val NOMINATORS_TITLE_RES = R.string.staking_active_delegators

private class ParachainNetworkInfoComponent(
    private val interactor: ParachainNetworkInfoInteractor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    resourceManager: ResourceManager,

    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
) : BaseNetworkInfoComponent(resourceManager, hostContext.scope) {

    private val delegatorStateFlow = hostContext.selectedAccount.flatMapLatest {
        delegatorStateUseCase.delegatorStateFlow(it, stakingOption.assetWithChain.chain, stakingOption.assetWithChain.asset)
    }.shareInBackground()

    init {
        updateContentState()

        updateExpandedState(with = expandForceChangeFlow())
    }

    override fun initialItems(): List<NetworkInfoItem> {
        return createNetworkInfoItems(
            totalStaked = null,
            minimumStake = null,
            activeNominators = null,
            unstakingPeriod = null,
            stakingPeriod = null,
            nominatorsLabel = NOMINATORS_TITLE_RES
        )
    }

    private fun expandForceChangeFlow(): Flow<Boolean> {
        return delegatorStateFlow.map { it is DelegatorState.None }
    }

    private fun updateContentState() {
        combine(
            hostContext.assetFlow,
            interactor.observeNetworkInfo(stakingOption.assetWithChain.chain.id)
        ) { asset, networkInfo ->
            val items = createNetworkInfoItems(asset, networkInfo, nominatorsLabel = NOMINATORS_TITLE_RES)

            updateState { it.copy(actions = items) }
        }
            .catch { Log.e(LOG_TAG, "Error while updating content state", it) }
            .inBackground()
            .launchIn(this)
    }
}
