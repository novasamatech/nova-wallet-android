package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.firstLoaded
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingPrimary
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingSecondary
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.syncingIf
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StakingDashboardViewModel(
    private val interactor: StakingDashboardInteractor,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val stakingDashboardUpdateSystem: StakingDashboardUpdateSystem,
    private val router: StakingRouter,
    private val stakingSharedState: StakingSharedState,
    private val presentationMapper: StakingDashboardPresentationMapper,
    private val dashboardUpdatePeriod: Duration = 200.milliseconds
) : BaseViewModel() {

    val walletUi = accountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val stakingDashboardFlow = interactor.stakingDashboardFlow()
        .shareInBackground()

    val stakingDashboardUiFlow = stakingDashboardFlow
        .throttleLast(dashboardUpdatePeriod)
        .map { dashboardLoading -> dashboardLoading.map(::mapDashboardToUi) }
        .shareInBackground()

    init {
        stakingDashboardUpdateSystem.start()
            .inBackground()
            .launchIn(this)
    }

    fun onHasStakeItemClicked(index: Int) = launch {
        val hasStakeItems = stakingDashboardFlow.firstLoaded().hasStake
        val hasStakeItem = hasStakeItems.getOrNull(index) ?: return@launch

        openChainStaking(
            chain = hasStakeItem.chain,
            chainAsset = hasStakeItem.token.configuration,
            stakingType = hasStakeItem.stakingState.stakingType
        )
    }

    fun onNoStakeItemClicked(index: Int) = launch {
        val withoutStakeItems = stakingDashboardFlow.firstLoaded().withoutStake
        val withoutStakeItem = withoutStakeItems.getOrNull(index) ?: return@launch
        val noStakeItemState = withoutStakeItem.stakingState as? NoStake ?: return@launch

        when (val flowType = noStakeItemState.flowType) {
            is NoStake.FlowType.Aggregated -> {} // TODO feature aggregated flows & nomination pools

            is NoStake.FlowType.Single -> openChainStaking(
                chain = withoutStakeItem.chain,
                chainAsset = withoutStakeItem.token.configuration,
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
            noStakeItems = dashboard.withoutStake.map(presentationMapper::mapWithoutStakeItemToUi),
        )
    }

    private fun mapHasStakeItemToUi(hasStake: AggregatedStakingDashboardOption<HasStake>): StakingDashboardModel.HasStakeItem {
        val stats = hasStake.stakingState.stats
        val isSyncingPrimary = hasStake.syncingStage.isSyncingPrimary()
        val isSyncingSecondary = hasStake.syncingStage.isSyncingSecondary()

        val stakingTypBadge = if (hasStake.stakingState.showStakingType) {
            presentationMapper.mapStakingTypeToUi(hasStake.stakingState.stakingType)
        } else {
            null
        }

        return StakingDashboardModel.HasStakeItem(
            chainUi = mapChainToUi(hasStake.chain).syncingIf(isSyncingPrimary),
            assetId = hasStake.token.configuration.id,
            rewards = stats.map { mapAmountToAmountModel(it.rewards, hasStake.token).syncingIf(isSyncingSecondary) },
            stake = mapAmountToAmountModel(hasStake.stakingState.stake, hasStake.token).syncingIf(isSyncingPrimary),
            status = stats.map { mapStakingStatusToUi(it.status).syncingIf(isSyncingSecondary) },
            earnings = stats.map { it.estimatedEarnings.format().syncingIf(isSyncingSecondary) },
            stakingTypeBadge = stakingTypBadge
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

        // TODO: open staking main fragment if we already have staking in chain
        router.openStartStakingFlow()
    }
}
