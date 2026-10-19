package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatter
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatterProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.firstLoaded
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.allStakingTypes
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingPrimary
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingSecondary
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.announcements.StakingAnnouncementsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.announcements.mapAnnouncementToUi
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapperFactory
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.SyncingData
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.syncingIf
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.combine
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NotYetResolved
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDashboardPendingNavigation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import io.novafoundation.nova.common.domain.dataOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val DASHBOARD_ANALYTICS_SOURCE = "dashboard"
private const val DEEP_LINK_ANALYTICS_SOURCE = "deep_link"

private class DashboardFormatters(
    val maskableValueFormatter: MaskableValueFormatter,
    val presentationMapper: StakingDashboardPresentationMapper
)

class StakingDashboardViewModel(
    private val interactor: StakingDashboardInteractor,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val stakingDashboardUpdateSystem: StakingDashboardUpdateSystem,
    private val dashboardRouter: StakingDashboardRouter,
    private val router: StakingRouter,
    private val startMultiStakingRouter: StartMultiStakingRouter,
    private val stakingSharedState: StakingSharedState,
    private val pendingNavigation: StakingDashboardPendingNavigation,
    private val presentationMapperFactory: StakingDashboardPresentationMapperFactory,
    private val dashboardUpdatePeriod: Duration = 200.milliseconds,
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val amountFormatter: AmountFormatter,
    private val assetIconProvider: AssetIconProvider,
    private val stakingAnnouncementsUseCase: StakingAnnouncementsUseCase,
    private val analyticsService: AnalyticsService
) : BaseViewModel() {

    private val dashboardFormattersFlow = maskableValueFormatterProvider.provideFormatter()
        .map { DashboardFormatters(it, presentationMapperFactory.create(it)) }
        .shareInBackground()

    val scrollToTopEvent = dashboardRouter.scrollToDashboardTopEvent

    val walletUi = accountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val stakingDashboardFlow = interactor.stakingDashboardFlow()
        .shareInBackground()

    val announcements = stakingAnnouncementsUseCase.generalAnnouncementsFlow()
        .map { announcements -> announcements.map(::mapAnnouncementToUi) }
        .shareInBackground()

    private val announcementsByChain = stakingAnnouncementsUseCase.announcementsByChainFlow()
        .shareInBackground()

    val stakingDashboardUiFlow = combine(
        stakingDashboardFlow.throttleLast(dashboardUpdatePeriod),
        dashboardFormattersFlow,
        announcementsByChain
    ) { dashboardLoading, valueFormatter, chainAnnouncements ->
        dashboardLoading.map { mapDashboardToUi(it, valueFormatter, chainAnnouncements) }
    }
        .shareInBackground()

    init {
        stakingDashboardUpdateSystem.start()
            .inBackground()
            .launchIn(this)

        pendingNavigation.target
            .filterNotNull()
            .onEach { openPendingStaking(it) }
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

        openStartStaking(
            chain = withoutStakeItem.chain,
            chainAsset = withoutStakeItem.token.configuration,
            noStake = noStakeItemState,
            analyticsSource = DASHBOARD_ANALYTICS_SOURCE
        )
    }

    fun onMoreOptionsClicked() {
        dashboardRouter.openMoreStakingOptions()
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    fun announcementLinkClicked(url: String) {
        router.openDAppBrowser(url)
    }

    private fun mapDashboardToUi(
        dashboard: StakingDashboard,
        formatters: DashboardFormatters,
        chainAnnouncements: Map<String, Announcement>
    ): StakingDashboardModel {
        return StakingDashboardModel(
            hasStakeItems = dashboard.hasStake.map { mapHasStakeItemToUi(it, formatters, chainAnnouncements) },
            noStakeItems = dashboard.withoutStake.map(formatters.presentationMapper::mapWithoutStakeItemToUi),
        )
    }

    private fun mapHasStakeItemToUi(
        hasStake: AggregatedStakingDashboardOption<HasStake>,
        formatters: DashboardFormatters,
        chainAnnouncements: Map<String, Announcement>
    ): StakingDashboardModel.HasStakeItem {
        val isSyncingPrimary = hasStake.syncingStage.isSyncingPrimary()
        val isSyncingSecondary = hasStake.syncingStage.isSyncingSecondary()
        val isBittensor = hasStake.stakingState.stakingType == Chain.Asset.StakingType.BITTENSOR_ROOT
        val stats = if (isBittensor) mapBittensorStats(formatters) else mapStakeStats(hasStake, formatters)

        val stakingTypeBadge = if (hasStake.stakingState.showStakingType) {
            formatters.presentationMapper.mapStakingTypeToUi(hasStake.stakingState.stakingType)
        } else {
            null
        }
        val stake = formatters.maskableValueFormatter.format {
            hasStake.stakingState.stake?.let { amountFormatter.formatAmountToAmountModel(it, hasStake.token) } ?: AmountModel("—", null)
        }.syncingIf(if (isBittensor) isSyncingPrimary else isSyncingSecondary)

        return StakingDashboardModel.HasStakeItem(
            assetLabel = resourceManager.getString(R.string.staking_rewards, hasStake.token.configuration.name).syncingIf(isSyncingPrimary),
            assetId = hasStake.token.configuration.fullId,
            rewards = stats.rewards,
            stake = stake,
            status = stats.status,
            earnings = stats.earnings,
            stakingTypeBadge = stakingTypeBadge,
            assetIcon = assetIconProvider.getAssetIconOrFallback(hasStake.token.configuration.icon).syncingIf(isSyncingPrimary),
            announcement = chainAnnouncements[hasStake.chain.id]?.let(::mapAnnouncementToUi)
        )
    }

    private fun mapStakeStats(hasStake: AggregatedStakingDashboardOption<HasStake>, formatters: DashboardFormatters): StakeStatsUi {
        val stats = hasStake.stakingState.stats
        val isSyncing = hasStake.syncingStage.isSyncingSecondary()
        return StakeStatsUi(
            rewards = stats.map { statistics ->
                formatters.maskableValueFormatter.format {
                    amountFormatter.formatAmountToAmountModel(statistics.rewards, hasStake.token)
                }.syncingIf(isSyncing)
            },
            status = stats.map { mapStakingStatusToUi(it.status).syncingIf(isSyncing) },
            earnings = stats.map { it.estimatedEarnings.format().syncingIf(isSyncing) }
        )
    }

    private fun mapBittensorStats(formatters: DashboardFormatters): StakeStatsUi {
        val rewards = formatters.maskableValueFormatter.format { AmountModel("—", null) }
        val status = StakeStatusModel(
            indicatorRes = R.drawable.ic_indicator_inactive_pulse,
            text = resourceManager.getString(R.string.bittensor_staking),
            textColorRes = R.color.text_primary
        )
        return StakeStatsUi(
            rewards = ExtendedLoadingState.Loaded(rewards.syncingIf(false)),
            status = ExtendedLoadingState.Loaded(status.syncingIf(false)),
            earnings = ExtendedLoadingState.Loaded("—".syncingIf(false))
        )
    }

    private class StakeStatsUi(
        val rewards: ExtendedLoadingState<SyncingData<MaskableModel<AmountModel>>>,
        val status: ExtendedLoadingState<SyncingData<StakeStatusModel>>,
        val earnings: ExtendedLoadingState<SyncingData<String>>
    )

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

    /**
     * A staking deep link lands on the dashboard first. Its asset opens once the dashboard has
     * resolved it, because until then an existing stake is still reported as [NotYetResolved].
     */
    private suspend fun openPendingStaking(target: FullChainAssetId) {
        val option = stakingDashboardFlow
            .mapNotNull { it.dataOrNull?.findResolvedOption(target) }
            .first()

        pendingNavigation.consume(target)

        when (val stakingState = option.stakingState) {
            is HasStake -> openChainStaking(option.chain, option.token.configuration, stakingState.stakingType)
            is NoStake -> openStartStaking(option.chain, option.token.configuration, stakingState, DEEP_LINK_ANALYTICS_SOURCE)
            else -> {}
        }
    }

    private fun StakingDashboard.findResolvedOption(target: FullChainAssetId): AggregatedStakingDashboardOption<*>? {
        hasStake.find { it.matches(target) }?.let { return it }

        return withoutStake.find { it.matches(target) }
            ?.takeIf { it.stakingState is NoStake }
    }

    private fun AggregatedStakingDashboardOption<*>.matches(target: FullChainAssetId): Boolean {
        return chain.id == target.chainId && token.configuration.id == target.assetId
    }

    private suspend fun openStartStaking(
        chain: Chain,
        chainAsset: Chain.Asset,
        noStake: NoStake,
        analyticsSource: String
    ) {
        val stakingTypes = noStake.flowType.allStakingTypes

        if (stakingTypes.singleOrNull() == Chain.Asset.StakingType.BITTENSOR_ROOT) {
            router.openBittensorRootStaking()
            return
        }

        stakingSharedState.setSelectedOption(chain, chainAsset, stakingTypes.first())

        analyticsService.track(AnalyticsEvent.StakingFlowOpened(network = chain.name, source = analyticsSource))

        val payload = StartStakingLandingPayload(
            availableStakingOptions = AvailableStakingOptionsPayload(chain.id, chainAsset.id, stakingTypes)
        )

        startMultiStakingRouter.openStartStakingLanding(payload)
    }

    private suspend fun openChainStaking(
        chain: Chain,
        chainAsset: Chain.Asset,
        stakingType: Chain.Asset.StakingType
    ) {
        if (stakingType == Chain.Asset.StakingType.BITTENSOR_ROOT) {
            router.openBittensorPortfolio()
            return
        }
        stakingSharedState.setSelectedOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingType = stakingType
        )

        router.openChainStakingMain()
    }
}
