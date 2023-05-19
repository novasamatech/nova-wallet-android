package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class StakingDashboardViewModel(
    private val interactor: StakingDashboardInteractor,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val stakingDashboardUpdateSystem: StakingDashboardUpdateSystem,
    private val router: StakingRouter,
) : BaseViewModel() {

    val walletUi = accountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    val stakingDashboardFlow = interactor.stakingDashboardFlow()
        .map(::mapDashboardToUi)
        .shareInBackground()

    init {
        stakingDashboardUpdateSystem.start()
            .launchIn(this)
    }

    fun onHasStakeItemClicked(index: Int) {
        // TODO
    }

    fun onNoStakeItemClicked(index: Int) {
        // TODO
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    private fun mapDashboardToUi(dashboard: StakingDashboard): StakingDashboardModel {
        return StakingDashboardModel(
            hasStakeItems = dashboard.hasStake.map(::mapHasStakeItemToUi),
            noStakeItems = dashboard.noStake.map(::mapNoStakeItemToUi),
            resolvingItems = dashboard.resolvingItems
        )
    }

    private fun mapHasStakeItemToUi(hasStake: AggregatedStakingDashboardOption<HasStake>): StakingDashboardModel.HasStakeItem {
        val stats = hasStake.stakingState.stats

        // we don't to show sync while also showing loading for stats
        val showSync = hasStake.syncing && stats is ExtendedLoadingState.Loaded

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

    private fun mapNoStakeItemToUi(noStake: AggregatedStakingDashboardOption<NoStake>): StakingDashboardModel.NoStakeItem {
        val stats = noStake.stakingState.stats
        val showSync = noStake.syncing && stats is ExtendedLoadingState.Loaded

        return StakingDashboardModel.NoStakeItem(
            chainUi = mapChainToUi(noStake.chain),
            assetId = noStake.token.configuration.id,
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
}
