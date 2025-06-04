package io.novafoundation.nova.feature_account_migration.di.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.splash.SplashPassedObserver
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_account_migration.presentation.deeplinks.MigrationCompleteDeepLinkHandler
import io.novafoundation.nova.feature_account_migration.presentation.deeplinks.RequestMigrationDeepLinkHandler
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideRequestMigrationDeepLinkHandler(
        router: AccountMigrationRouter,
        automaticInteractionGate: AutomaticInteractionGate,
        splashPassedObserver: SplashPassedObserver,
        repository: AccountRepository
    ) = RequestMigrationDeepLinkHandler(
        router,
        automaticInteractionGate,
        splashPassedObserver,
        repository
    )

    @Provides
    @FeatureScope
    fun provideMigrationCompleteDeepLinkHandler(
        automaticInteractionGate: AutomaticInteractionGate,
        accountMigrationMixinProvider: AccountMigrationMixinProvider,
        repository: AccountRepository
    ) = MigrationCompleteDeepLinkHandler(
        automaticInteractionGate,
        accountMigrationMixinProvider,
        repository
    )

    @Provides
    @FeatureScope
    fun provideDeepLinks(
        requestMigrationHandler: RequestMigrationDeepLinkHandler,
        migrationCompleteHandler: MigrationCompleteDeepLinkHandler
    ): AccountMigrationDeepLinks {
        return AccountMigrationDeepLinks(listOf(requestMigrationHandler, migrationCompleteHandler))
    }
}
