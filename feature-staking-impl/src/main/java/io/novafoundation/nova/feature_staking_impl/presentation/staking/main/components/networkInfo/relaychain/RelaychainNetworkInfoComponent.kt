package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoItem.Content
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.activeNominators
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.minimumStake
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.stakingPeriod
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.totalStaked
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.unstakingPeriod
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RelaychainNetworkInfoComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val resourceManager: ResourceManager,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext
    ): NetworkInfoComponent = RelaychainNetworkInfoComponent(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        assetWithChain = assetWithChain,
        hostContext = hostContext
    )
}

private class RelaychainNetworkInfoComponent(
    private val stakingInteractor: StakingInteractor,
    private val resourceManager: ResourceManager,

    private val hostContext: ComponentHostContext,
    private val assetWithChain: SingleAssetSharedState.AssetWithChain,
) : NetworkInfoComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    override val state = MutableStateFlow(initialState())

    private val selectedAccountStakingStateFlow = hostContext.selectedAccount.flatMapLatest {
        stakingInteractor.selectedAccountStakingStateFlow(it, assetWithChain)
    }.shareInBackground()

    init {
        updateExpandedState()

        updateContentState()
    }

    override fun onAction(action: NetworkInfoAction) {
        when (action) {
            NetworkInfoAction.ChangeExpendedStateClicked -> updateExpanded { !it.expanded }
        }
    }

    private fun updateContentState() {
        combine(
            hostContext.assetFlow,
            stakingInteractor.observeNetworkInfoState(assetWithChain.chain.id)
        ) { asset, networkInfo ->
            val items = createNetworkInfoItems(asset, networkInfo)

            updateState { it.copy(actions = items) }
        }
            .inBackground()
            .launchIn(this)
    }

    private fun createNetworkInfoItems(asset: Asset, networkInfo: NetworkInfo): List<NetworkInfoItem> {
        val unstakingPeriod = resourceManager.getQuantityString(R.plurals.staking_main_lockup_period_value, networkInfo.lockupPeriodInDays)
            .format(networkInfo.lockupPeriodInDays)

        val stakingPeriod = when (networkInfo.stakingPeriod) {
            StakingPeriod.Unlimited -> resourceManager.getString(R.string.common_unlimited)
        }

        return createNetworkInfoItems(
            totalStaked = mapAmountToAmountModel(networkInfo.totalStake, asset),
            minimumStake = mapAmountToAmountModel(networkInfo.minimumStake, asset),
            activeNominators = networkInfo.nominatorsCount.format(),
            unstakingPeriod = unstakingPeriod,
            stakingPeriod = stakingPeriod
        )
    }

    private fun initialState(): NetworkInfoState {
        return NetworkInfoState(
            actions = createNetworkInfoItems(
                totalStaked = null,
                minimumStake = null,
                activeNominators = null,
                unstakingPeriod = null,
                stakingPeriod = null
            ),
            expanded = false
        )
    }

    private fun updateExpandedState() {
        selectedAccountStakingStateFlow.onEach { stakingState ->
            updateExpanded { stakingState is StakingState.NonStash }
        }
            .inBackground()
            .launchIn(this)
    }

    private fun updateExpanded(expanded: (NetworkInfoState) -> Boolean) = updateState {
        it.copy(expanded = expanded(it))
    }

    private fun updateState(update: (NetworkInfoState) -> NetworkInfoState) {
        state.value = update(state.value)
    }

    private fun createNetworkInfoItems(
        totalStaked: AmountModel?,
        minimumStake: AmountModel?,
        activeNominators: String?,
        unstakingPeriod: String?,
        stakingPeriod: String?
    ): List<NetworkInfoItem> {
        return listOf(
            NetworkInfoItem.totalStaked(resourceManager, totalStaked.toNetworkInfoContent()),
            NetworkInfoItem.minimumStake(resourceManager, minimumStake.toNetworkInfoContent()),
            NetworkInfoItem.activeNominators(resourceManager, activeNominators.toNetworkInfoContent()),
            NetworkInfoItem.stakingPeriod(resourceManager, stakingPeriod.toNetworkInfoContent()),
            NetworkInfoItem.unstakingPeriod(resourceManager, unstakingPeriod.toNetworkInfoContent())
        )
    }

    private fun String?.toNetworkInfoContent() = this?.let { Content(primary = this, secondary = null) }
    private fun AmountModel?.toNetworkInfoContent() = this?.let { Content(primary = token, secondary = fiat) }
}
