package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
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
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapperFactory
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.syncingIf
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatter
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatterProvider
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.subtensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val amountFormatter: AmountFormatter,
    private val assetIconProvider: AssetIconProvider
) : BaseViewModel() {

    private val dashboardFormattersFlow = maskableValueFormatterProvider.provideFormatter()
        .map { DashboardFormatters(it, presentationMapperFactory.create(it)) }
        .shareInBackground()

    val scrollToTopEvent = dashboardRouter.scrollToDashboardTopEvent

    val walletUi = accountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val stakingDashboardFlow = interactor.stakingDashboardFlow()
        .shareInBackground()

    val stakingDashboardUiFlow = stakingDashboardFlow
        .throttleLast(dashboardUpdatePeriod)
        .combine(dashboardFormattersFlow) { dashboardLoading, valueFormatter -> dashboardLoading.map { mapDashboardToUi(it, valueFormatter) } }
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

        val stakingTypes = noStakeItemState.flowType.allStakingTypes
        val chain = withoutStakeItem.chain
        val chainAsset = withoutStakeItem.token.configuration
        val firstStakingType = stakingTypes.first()

        // Subtensor doesn't fit the start-staking landing flow (no `RewardCalculator`
        // for SUBTENSOR yet) — route straight to the detail screen instead.
        // Mirrors iOS `StakingDashboardWireframe.showSubtensorStakingDetails`:
        // normalize to the native TAO asset before publishing to shared state
        // so balance / fee / signing flows always operate on TAO. The entry
        // netuid is preserved separately so the detail screen can scope.
        if (firstStakingType == Chain.Asset.StakingType.SUBTENSOR) {
            val taoAsset = chain.subtensorTaoAssetOrNull() ?: return@launch
            stakingSharedState.setSelectedOption(
                chain = chain,
                chainAsset = taoAsset,
                stakingType = firstStakingType,
                subtensorEntryNetuid = chainAsset.subtensorNetuid(),
            )
            router.openSubtensorStakingMain()
            return@launch
        }

        stakingSharedState.setSelectedOption(chain, chainAsset, firstStakingType)

        val payload = StartStakingLandingPayload(
            availableStakingOptions = AvailableStakingOptionsPayload(chain.id, chainAsset.id, stakingTypes)
        )

        startMultiStakingRouter.openStartStakingLanding(payload)
    }

    fun onMoreOptionsClicked() {
        dashboardRouter.openMoreStakingOptions()
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    private fun mapDashboardToUi(dashboard: StakingDashboard, formatters: DashboardFormatters): StakingDashboardModel {
        return StakingDashboardModel(
            hasStakeItems = dashboard.hasStake.map { mapHasStakeItemToUi(it, formatters) },
            noStakeItems = dashboard.withoutStake.map(formatters.presentationMapper::mapWithoutStakeItemToUi),
        )
    }

    private fun mapHasStakeItemToUi(
        hasStake: AggregatedStakingDashboardOption<HasStake>,
        formatters: DashboardFormatters
    ): StakingDashboardModel.HasStakeItem {
        val stats = hasStake.stakingState.stats
        val isSyncingPrimary = hasStake.syncingStage.isSyncingPrimary()
        val isSyncingSecondary = hasStake.syncingStage.isSyncingSecondary()

        val stakingTypBadge = if (hasStake.stakingState.showStakingType) {
            formatters.presentationMapper.mapStakingTypeToUi(hasStake.stakingState.stakingType)
        } else {
            null
        }

        return StakingDashboardModel.HasStakeItem(
            assetLabel = resourceManager.getString(R.string.staking_rewards, hasStake.token.configuration.name).syncingIf(isSyncingPrimary),
            assetId = hasStake.token.configuration.fullId,
            rewards = stats.map {
                formatters.maskableValueFormatter.format {
                    amountFormatter.formatAmountToAmountModel(it.rewards, hasStake.token)
                }.syncingIf(isSyncingSecondary)
            },
            stake = formatters.maskableValueFormatter.format {
                amountFormatter.formatAmountToAmountModel(hasStake.stakingState.stake, hasStake.token)
            }.syncingIf(isSyncingSecondary),
            status = stats.map { mapStakingStatusToUi(it.status).syncingIf(isSyncingSecondary) },
            earnings = stats.map { it.estimatedEarnings.format().syncingIf(isSyncingSecondary) },
            stakingTypeBadge = stakingTypBadge,
            assetIcon = assetIconProvider.getAssetIconOrFallback(hasStake.token.configuration.icon).syncingIf(isSyncingPrimary)
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
        if (stakingType == Chain.Asset.StakingType.SUBTENSOR) {
            // Mirrors iOS `StakingDashboardWireframe.showSubtensorStakingDetails`:
            // normalize the entry chainAsset to the native TAO asset, but
            // preserve the originally tapped row's netuid so the detail
            // screen scopes correctly (e.g. tapping SN8 → entryNetuid 8).
            val taoAsset = chain.subtensorTaoAssetOrNull() ?: return
            stakingSharedState.setSelectedOption(
                chain = chain,
                chainAsset = taoAsset,
                stakingType = stakingType,
                subtensorEntryNetuid = chainAsset.subtensorNetuid(),
            )
            router.openSubtensorStakingMain()
        } else {
            stakingSharedState.setSelectedOption(
                chain = chain,
                chainAsset = chainAsset,
                stakingType = stakingType
            )
            router.openChainStakingMain()
        }
    }
}

/**
 * Returns the chain's native TAO asset (utility asset) when the chain
 * supports Subtensor staking, otherwise null. Mirrors the iOS extension
 * `chainAsset.subtensorTaoAsset()` used by `StakingDashboardWireframe`.
 */
private fun Chain.subtensorTaoAssetOrNull(): Chain.Asset? {
    val utility = utilityAsset
    return utility.takeIf { Chain.Asset.StakingType.SUBTENSOR in it.staking }
}
