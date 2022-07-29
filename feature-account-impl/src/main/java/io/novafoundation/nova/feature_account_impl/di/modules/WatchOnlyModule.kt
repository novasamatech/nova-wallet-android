package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_impl.data.repository.RealWatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.WatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.sign.RealWatchOnlyMissingKeysPresenter

@Module
class WatchOnlyModule {

    @Provides
    @FeatureScope
    fun provideWatchOnlySigningPresenter(
        contextManager: ContextManager
    ): WatchOnlyMissingKeysPresenter = RealWatchOnlyMissingKeysPresenter(contextManager)

    @Provides
    @FeatureScope
    fun provideWatchOnlyRepository(
        accountDao: MetaAccountDao
    ): WatchOnlyRepository = RealWatchOnlyRepository(accountDao)
}
