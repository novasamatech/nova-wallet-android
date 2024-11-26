package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MoreStakingOptions
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDApp
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.allStakingTypes
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.MoreStakingOptionsModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.StakingDAppModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MoreStakingOptionsViewModel(
    private val interactor: StakingDashboardInteractor,
    private val startStakingRouter: StartMultiStakingRouter,
    private val dashboardRouter: StakingDashboardRouter,
    private val stakingSharedState: StakingSharedState,
    private val presentationMapper: StakingDashboardPresentationMapper,
    private val dappRouter: DAppRouter
) : BaseViewModel() {

    init {
        syncDApps()
    }

    private val moreStakingOptionsFlow = interactor.moreStakingOptionsFlow()
        .shareInBackground()

    val moreStakingOptionsUiFlow = moreStakingOptionsFlow
        .map(::mapMoreOptionsToUi)
        .shareInBackground()

    fun onInAppStakingItemClicked(index: Int) = launch {
        val withoutStakeItems = moreStakingOptionsFlow.first().inAppStaking
        val withoutStakeItem = withoutStakeItems.getOrNull(index) ?: return@launch

        val noStakeItemState = withoutStakeItem.stakingState as? AggregatedStakingDashboardOption.NoStake ?: return@launch

        val stakingTypes = noStakeItemState.flowType.allStakingTypes

        openChainStaking(withoutStakeItem.chain, withoutStakeItem.token.configuration, stakingTypes)
    }

    fun onBrowserStakingItemClicked(item: StakingDAppModel) = launch {
        dappRouter.openDAppBrowser(DAppBrowserPayload.Address(item.url))
    }

    private fun syncDApps() = launch {
        interactor.syncDapps()
    }

    private fun mapMoreOptionsToUi(moreStakingOptions: MoreStakingOptions): MoreStakingOptionsModel {
        return MoreStakingOptionsModel(
            inAppStaking = moreStakingOptions.inAppStaking.map(presentationMapper::mapWithoutStakeItemToUi),
            browserStaking = moreStakingOptions.browserStaking.map { dApps -> dApps.map(::mapStakingDAppToUi) }
        )
    }

    private fun mapStakingDAppToUi(stakingDApp: StakingDApp): StakingDAppModel {
        return with(stakingDApp) {
            StakingDAppModel(url = url, iconUrl = iconUrl, name = name)
        }
    }

    private suspend fun openChainStaking(chain: Chain, chainAsset: Chain.Asset, stakingTypes: List<Chain.Asset.StakingType>) {
        stakingSharedState.setSelectedOption(chain, chainAsset, stakingTypes.first())

        val payload = StartStakingLandingPayload(AvailableStakingOptionsPayload(chain.id, chainAsset.id, stakingTypes))

        startStakingRouter.openStartStakingLanding(payload)
    }

    fun goBack() {
        dashboardRouter.backInStakingTab()
    }
}
