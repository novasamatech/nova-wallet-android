package io.novafoundation.nova.feature_ahm_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_ahm_api.di.deeplinks.ChainMigrationDeepLinks
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.deeplink.ChainMigrationDetailsDeepLinkHandler

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideStakingDashboardDeepLinkHandler(
        router: ChainMigrationRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = ChainMigrationDetailsDeepLinkHandler(
        router,
        automaticInteractionGate
    )

    @Provides
    @FeatureScope
    fun provideDeepLinks(stakingDashboard: ChainMigrationDetailsDeepLinkHandler): ChainMigrationDeepLinks {
        return ChainMigrationDeepLinks(listOf(stakingDashboard))
    }
}
