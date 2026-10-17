package io.novafoundation.nova.feature_staking_impl.di.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_staking_api.di.deeplinks.StakingDeepLinks
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDashboardDeepLinkHandler
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDashboardPendingNavigation
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTargetResolver
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideStakingDeepLinkTargetResolver(chainRegistry: ChainRegistry) = StakingDeepLinkTargetResolver(chainRegistry)

    @Provides
    @FeatureScope
    fun provideStakingDashboardPendingNavigation() = StakingDashboardPendingNavigation()

    @Provides
    @FeatureScope
    fun provideStakingDashboardDeepLinkHandler(
        router: StakingRouter,
        targetResolver: StakingDeepLinkTargetResolver,
        pendingNavigation: StakingDashboardPendingNavigation,
        automaticInteractionGate: AutomaticInteractionGate
    ) = StakingDashboardDeepLinkHandler(
        router,
        targetResolver,
        pendingNavigation,
        automaticInteractionGate
    )

    @Provides
    @FeatureScope
    fun provideDeepLinks(stakingDashboard: StakingDashboardDeepLinkHandler): StakingDeepLinks {
        return StakingDeepLinks(listOf(stakingDashboard))
    }
}
