package io.novafoundation.nova.app.root.analytics

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.FeatureId
import io.novafoundation.nova.app.R
import io.novafoundation.nova.common.di.scope.FeatureScope
import javax.inject.Inject

/**
 * Reports which product section the user opened, wherever they opened it from.
 *
 * Destinations arrive from more than one navigation host — the root graph and the
 * split screen both launch major sections — so the mapping and the de-duplication
 * live here once instead of in each host. Repeated destinations inside the same
 * section are collapsed: `feature_opened` answers "did they get to staking", not
 * "how many screens deep did they go".
 */
@FeatureScope
class FeatureNavigationTracker @Inject constructor(
    private val analyticsService: AnalyticsService
) {

    @Volatile
    private var currentFeature: FeatureId? = null

    fun onDestinationChanged(destinationId: Int) {
        if (destinationId == R.id.novaCardFragment) {
            analyticsService.track(AnalyticsEvent.NovaCardOpened)
        }

        val feature = featureFor(destinationId)

        // Screens outside any tracked section leave the current one in place, so
        // returning to it from a detail screen does not re-report it.
        if (feature == null || feature == currentFeature) return

        currentFeature = feature
        analyticsService.track(AnalyticsEvent.FeatureOpened(feature))
    }

    private fun featureFor(destinationId: Int): FeatureId? = when (destinationId) {
        R.id.stakingDashboardFragment, R.id.stakingFragment, R.id.startStakingLandingFragment -> FeatureId.STAKING
        R.id.voteFragment, R.id.referendumDetailsFragment, R.id.delegateListFragment -> FeatureId.GOVERNANCE
        R.id.userContributionsFragment, R.id.crowdloanContributeFragment -> FeatureId.CROWDLOANS
        R.id.dAppsFragment, R.id.dappBrowserFragment, R.id.dappSearchFragment, R.id.dappTabsFragment -> FeatureId.DAPPS
        R.id.nftListFragment -> FeatureId.NFT
        R.id.swapSettingsFragment -> FeatureId.SWAP
        R.id.buyFlowFragment, R.id.buyFlowNetworkFragment -> FeatureId.BUY
        R.id.sendFlowFragment, R.id.sendFlowNetworkFragment -> FeatureId.SEND
        R.id.receiveFlowFragment, R.id.receiveFlowNetworkFragment, R.id.receiveFragment -> FeatureId.RECEIVE
        R.id.profileFragment -> FeatureId.SETTINGS
        else -> null
    }
}
