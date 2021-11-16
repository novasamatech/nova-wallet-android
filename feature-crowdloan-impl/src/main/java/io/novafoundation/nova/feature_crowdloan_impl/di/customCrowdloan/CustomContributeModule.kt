package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.acala.AcalaContributionModule
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.astar.AstarContributionModule
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.bifrost.BifrostContributionModule
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.moonbeam.MoonbeamContributionModule
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.parallel.ParallelContributionModule

@Module(
    includes = [
        AcalaContributionModule::class,
        BifrostContributionModule::class,
        MoonbeamContributionModule::class,
        AstarContributionModule::class,
        ParallelContributionModule::class
    ]
)
class CustomContributeModule {

    @Provides
    @FeatureScope
    fun provideCustomContributionManager(
        factories: @JvmSuppressWildcards Set<CustomContributeFactory>,
    ) = CustomContributeManager(factories)
}
