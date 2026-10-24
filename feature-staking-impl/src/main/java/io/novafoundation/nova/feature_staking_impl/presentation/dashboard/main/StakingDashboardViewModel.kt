package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.dataOrNull
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
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.mapDashboardStakingStatus
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkDestination
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTarget
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.resolveStakingDeepLink
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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    private val presentationMapperFactory: StakingDashboardPresentationMapperFactory,
    private val dashboardUpdatePeriod: Duration = 200.milliseconds,
    private val pendingDeepLinkTimeout: Duration = 10.seconds,
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

    private val maximumYieldRefreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val stakingDashboardFlow = interactor.stakingDashboardFlow(maximumYieldRefreshes)
        .shareInBackground()

    private val pendingDeepLinkTarget = MutableStateFlow<StakingDeepLinkTarget?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val deepLinkDestination = pendingDeepLinkTarget.flatMapLatest { target ->
        if (target == null) flowOf(null) else pendingDeepLinkDestination(target)
    }
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
    }

    fun onHasStakeItemClicked(index: Int) = launch {
        cancelDeepLink()
        val hasStakeItems = stakingDashboardFlow.firstLoaded().hasStake
        val hasStakeItem = hasStakeItems.getOrNull(index) ?: return@launch

        openChainStaking(
            chain = hasStakeItem.chain,
            chainAsset = hasStakeItem.token.configuration,
            stakingType = hasStakeItem.stakingState.stakingType
        )
    }

    fun onNoStakeItemClicked(index: Int) = launch {
        cancelDeepLink()
        val withoutStakeItems = stakingDashboardFlow.firstLoaded().withoutStake
        val withoutStakeItem = withoutStakeItems.getOrNull(index) ?: return@launch
        val noStakeItemState = withoutStakeItem.stakingState as? NoStake ?: return@launch

        openStartStaking(withoutStakeItem.chain, withoutStakeItem.token.configuration, noStakeItemState, StakingFlowSource.DASHBOARD)
    }

    fun setDeepLinkTarget(target: StakingDeepLinkTarget) {
        pendingDeepLinkTarget.value = target
    }

    fun cancelDeepLink() {
        pendingDeepLinkTarget.value = null
    }

    fun onResume() {
        maximumYieldRefreshes.tryEmit(Unit)
    }

    /**
     * As iOS `StakingDashboardPresenter`: waits until the dashboard settles the item, and once [pendingDeepLinkTimeout]
     * passes takes the item as it is or drops the link.
     */
    private fun pendingDeepLinkDestination(target: StakingDeepLinkTarget) = flow<StakingDeepLinkDestination?> {
        val settled = withTimeoutOrNull(pendingDeepLinkTimeout) {
            stakingDashboardFlow.mapNotNull { state -> state.dataOrNull?.let { resolveStakingDeepLink(target, it, waitingForSync = true) } }
                .first()
        }
        val destination = settled ?: stakingDashboardFlow.replayCache.lastOrNull()?.dataOrNull
            ?.let { resolveStakingDeepLink(target, it, waitingForSync = false) }

        if (destination != null) emit(destination) else pendingDeepLinkTarget.compareAndSet(target, null)
    }

    fun openDeepLinkDestination(destination: StakingDeepLinkDestination) {
        if (pendingDeepLinkTarget.value != destination.target) return
        pendingDeepLinkTarget.value = null
        launch {
            val option = destination.option
            when (val state = option.stakingState) {
                is HasStake -> openChainStaking(option.chain, option.token.configuration, state.stakingType)
                is NoStake -> openStartStaking(option.chain, option.token.configuration, state, StakingFlowSource.DEEP_LINK)
            }
        }
    }

    private suspend fun openStartStaking(chain: Chain, chainAsset: Chain.Asset, state: NoStake, source: StakingFlowSource) {
        val stakingTypes = state.flowType.allStakingTypes

        stakingSharedState.setSelectedOption(chain, chainAsset, stakingTypes.first())

        val hasNewBadge = if (source == StakingFlowSource.DASHBOARD) chain.additional?.newFeatures?.contains(Chain.NewFeature.STAKING) == true else null
        analyticsService.track(AnalyticsEvent.StakingFlowOpened(network = chain.name, source = source.value, hasNewBadge = hasNewBadge))

        val payload = StartStakingLandingPayload(
            availableStakingOptions = AvailableStakingOptionsPayload(chain.id, chainAsset.id, stakingTypes)
        )

        startMultiStakingRouter.openStartStakingLanding(payload)
    }

    fun onMoreOptionsClicked() {
        cancelDeepLink()
        dashboardRouter.openMoreStakingOptions()
    }

    fun avatarClicked() {
        cancelDeepLink()
        router.openSwitchWallet()
    }

    fun announcementLinkClicked(url: String) {
        cancelDeepLink()
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
        formatters.presentationMapper.mapDetailedStakeItemToUi(
            hasStake,
            amountFormatter,
            chainAnnouncements[hasStake.chain.id]?.let(::mapAnnouncementToUi)
        )?.let { return it }
        val isSyncingPrimary = hasStake.syncingStage.isSyncingPrimary()
        val isSyncingSecondary = hasStake.syncingStage.isSyncingSecondary()
        val stats = mapStakeStats(hasStake, formatters)

        val stakingTypeBadge = if (hasStake.stakingState.showStakingType) {
            formatters.presentationMapper.mapStakingTypeToUi(hasStake.stakingState.stakingType)
        } else {
            null
        }
        val stake = formatters.maskableValueFormatter.format {
            hasStake.stakingState.stake?.let { amountFormatter.formatAmountToAmountModel(it, hasStake.token) } ?: AmountModel("—", null)
        }.syncingIf(isSyncingSecondary)

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

    private class StakeStatsUi(
        val rewards: ExtendedLoadingState<SyncingData<MaskableModel<AmountModel>>>,
        val status: ExtendedLoadingState<SyncingData<StakeStatusModel>>,
        val earnings: ExtendedLoadingState<SyncingData<String>>
    )

    private fun mapStakingStatusToUi(stakingStatus: HasStake.StakingStatus): StakeStatusModel {
        return mapDashboardStakingStatus(stakingStatus, resourceManager)
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
