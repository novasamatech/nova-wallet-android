package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MoreStakingOptions
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDApp
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.mapNoStakeItemToUi
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.MoreStakingOptionsModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.StakingDAppModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MoreStakingOptionsViewModel(
    private val interactor: StakingDashboardInteractor,
    private val router: StakingRouter,
    private val stakingSharedState: StakingSharedState
) : BaseViewModel() {

    init {
        syncDApps()
    }

    private val moreStakingOptionsFlow = interactor.moreStakingOptionsFlow()
        .inBackground()

    val moreStakingOptionsUiFlow = moreStakingOptionsFlow
        .map(::mapMoreOptionsToUi)
        .shareInBackground()

    fun onInAppStakingItemClicked(index: Int) = launch {
        val inAppStakingItems = moreStakingOptionsFlow.first().inAppStaking
        val inAppStakingItem = inAppStakingItems.getOrNull(index) ?: return@launch

        when (val flowType = inAppStakingItem.stakingState.flowType) {
            NoStake.FlowType.Aggregated -> {} // TODO feature aggregated flows & nomination pools

            is NoStake.FlowType.Single -> openChainStaking(
                chain = inAppStakingItem.chain,
                chainAsset = inAppStakingItem.token.configuration,
                stakingType = flowType.stakingType
            )
        }
    }

    fun onBrowserStakingItemClicked(item: StakingDAppModel) = launch {
        router.openDAppBrowser(item.url)
    }

    private fun syncDApps() = launch {
        interactor.syncDapps()
    }

    private fun mapMoreOptionsToUi(moreStakingOptions: MoreStakingOptions): MoreStakingOptionsModel {
        return MoreStakingOptionsModel(
            inAppStaking = moreStakingOptions.inAppStaking.map(::mapNoStakeItemToUi),
            resolvingInAppItems = moreStakingOptions.resolvingInAppItems,
            browserStaking = moreStakingOptions.browserStaking.map { dApps -> dApps.map(::mapStakingDAppToUi) }
        )
    }

    private fun mapStakingDAppToUi(stakingDApp: StakingDApp): StakingDAppModel {
        return with(stakingDApp) {
            StakingDAppModel(url = url, iconUrl = iconUrl, name = name)
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

    fun goBack() {
        router.backInStakingTab()
    }
}
