package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain

import android.util.Log
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.BaseNetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class RelaychainNetworkInfoComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val stakingSharedComputation: StakingSharedComputation,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): NetworkInfoComponent = RelaychainNetworkInfoComponent(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
        stakingSharedComputation = stakingSharedComputation
    )
}

private val NOMINATORS_TITLE_RES = R.string.staking_main_active_nominators_title

private class RelaychainNetworkInfoComponent(
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedComputation: StakingSharedComputation,
    resourceManager: ResourceManager,

    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
) : BaseNetworkInfoComponent(resourceManager, hostContext.scope) {

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = stakingOption.assetWithChain,
        scope = hostContext.scope
    )

    init {
        updateContentState()

        updateExpandedState(with = expandForceChangeFlow())
    }

    override fun initialItems(): List<NetworkInfoItem> {
        return createNetworkInfoItems(nominatorsLabel = NOMINATORS_TITLE_RES)
    }

    private fun expandForceChangeFlow(): Flow<Boolean> {
        return selectedAccountStakingStateFlow.map { it is StakingState.NonStash }
    }

    private fun updateContentState() {
        combine(
            hostContext.assetFlow,
            stakingInteractor.observeNetworkInfoState(stakingOption.assetWithChain.chain.id, hostContext.scope)
        ) { asset, networkInfo ->
            val items = createNetworkInfoItems(asset, networkInfo, nominatorsLabel = NOMINATORS_TITLE_RES)

            updateState { it.copy(actions = items) }
        }
            .catch { Log.e(LOG_TAG, "Error while updating content state", it) }
            .inBackground()
            .launchIn(this)
    }
}
