package io.novafoundation.nova.feature_staking_impl.di.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_staking_api.di.deeplinks.StakingDeepLinks
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDashboardDeepLinkHandler

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideStakingDashboardDeepLinkHandler(
        router: StakingRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = StakingDashboardDeepLinkHandler(
        router,
        automaticInteractionGate
    )

    @Provides
    @FeatureScope
    fun provideDeepLinks(stakingDashboard: StakingDashboardDeepLinkHandler): StakingDeepLinks {
        return StakingDeepLinks(listOf(stakingDashboard))
    }
}
