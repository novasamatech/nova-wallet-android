package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_impl.data.repository.RealWatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.WatchOnlyRepository

@Module
class WatchOnlyModule {

    @Provides
    @FeatureScope
    fun provideWatchOnlyRepository(
        accountDao: MetaAccountDao
    ): WatchOnlyRepository = RealWatchOnlyRepository(accountDao)
}
