package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StakingDashboardViewModel(
    private val interactor: StakingDashboardInteractor,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val stakingDashboardUpdateSystem: StakingDashboardUpdateSystem,
    private val router: StakingRouter,
    private val stakingSharedState: StakingSharedState,
    private val presentationMapper: StakingDashboardPresentationMapper,
) : BaseViewModel() {

    val walletUi = accountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val stakingDashboardFlow = interactor.stakingDashboardFlow()
        .shareInBackground()

    val stakingDashboardUiFlow = stakingDashboardFlow
        .map(::mapDashboardToUi)
        .shareInBackground()

    init {
        stakingDashboardUpdateSystem.start()
            .inBackground()
            .launchIn(this)
    }

    fun onHasStakeItemClicked(index: Int) = launch {
        val hasStakeItems = stakingDashboardFlow.first().hasStake
        val hasStakeItem = hasStakeItems.getOrNull(index) ?: return@launch

        openChainStaking(
            chain = hasStakeItem.chain,
            chainAsset = hasStakeItem.token.configuration,
            stakingType = hasStakeItem.stakingState.stakingType
        )
    }

    fun onNoStakeItemClicked(index: Int) = launch {
        val noStakeItems = stakingDashboardFlow.first().noStake
        val noStakeItem = noStakeItems.getOrNull(index) ?: return@launch

        when (val flowType = noStakeItem.stakingState.flowType) {
            is NoStake.FlowType.Aggregated -> {} // TODO feature aggregated flows & nomination pools

            is NoStake.FlowType.Single -> openChainStaking(
                chain = noStakeItem.chain,
                chainAsset = noStakeItem.token.configuration,
                stakingType = flowType.stakingType
            )
        }
    }

    fun onMoreOptionsClicked() {
        router.openMoreStakingOptions()
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    private fun mapDashboardToUi(dashboard: StakingDashboard): StakingDashboardModel {
        return StakingDashboardModel(
            hasStakeItems = dashboard.hasStake.map(::mapHasStakeItemToUi),
            noStakeItems = dashboard.noStake.map(presentationMapper::mapNoStakeItemToUi),
            resolvingItems = dashboard.resolvingItems
        )
    }

    private fun mapHasStakeItemToUi(hasStake: AggregatedStakingDashboardOption<HasStake>): StakingDashboardModel.HasStakeItem {
        val stats = hasStake.stakingState.stats

        // we don't to show sync while also showing loading for stats
        val showSync = hasStake.syncingStage && stats is ExtendedLoadingState.Loaded

        return StakingDashboardModel.HasStakeItem(
            chainUi = mapChainToUi(hasStake.chain),
            assetId = hasStake.token.configuration.id,
            rewards = stats.map { mapAmountToAmountModel(it.rewards, hasStake.token) },
            stake = mapAmountToAmountModel(hasStake.stakingState.stake, hasStake.token),
            status = stats.map { mapStakingStatusToUi(it.status) },
            earnings = stats.map { it.estimatedEarnings.format() },
            syncing = showSync
        )
    }

    private fun mapStakingStatusToUi(stakingStatus: HasStake.StakingStatus): StakeStatusModel {
        return when (stakingStatus) {
            HasStake.StakingStatus.ACTIVE -> StakeStatusModel(
                indicatorRes = R.drawable.ic_indicator_positive_pulse,
                text = resourceManager.getString(R.string.common_active),
                textColorRes = R.color.text_positive
            )

            HasStake.StakingStatus.INACTIVE -> StakeStatusModel(
                indicatorRes = R.drawable.ic_indicator_negative_pulse,
                text = resourceManager.getString(R.string.staking_nominator_status_inactive),
                textColorRes = R.color.text_negative
            )

            HasStake.StakingStatus.WAITING -> StakeStatusModel(
                indicatorRes = R.drawable.ic_indicator_inactive_pulse,
                text = resourceManager.getString(R.string.common_waiting),
                textColorRes = R.color.text_primary
            )
        }
    }

    private suspend fun openChainStaking(
        chain: Chain,
        chainAsset: Chain.Asset,
        stakingType: Chain.Asset.StakingType
    ) {
        stakingSharedState.setSelectedOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingType = stakingType
        )

        router.openChainStakingMain()
    }
}
