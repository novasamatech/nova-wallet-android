package jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan

import dagger.Module
import dagger.Provides
import jp.co.soramitsu.common.di.scope.FeatureScope
import jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan.acala.AcalaContributionModule
import jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan.bifrost.BifrostContributionModule

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
