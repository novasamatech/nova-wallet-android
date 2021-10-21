package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.acala.AcalaContributionModule
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.bifrost.BifrostContributionModule

@Module(
    includes = [
        AcalaContributionModule::class,
        BifrostContributionModule::class
    ]
)
class CustomContributeModule {

    @Provides
    @FeatureScope
    fun provideCustomContributionManager(
        factories: @JvmSuppressWildcards Set<CustomContributeFactory>
    ) = CustomContributeManager(factories)
}
