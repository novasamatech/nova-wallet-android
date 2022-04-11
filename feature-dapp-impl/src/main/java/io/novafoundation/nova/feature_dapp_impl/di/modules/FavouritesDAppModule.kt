package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.FavouriteDAppsDao
import io.novafoundation.nova.feature_dapp_impl.data.repository.DbFavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository

@Module
class FavouritesDAppModule {

    @Provides
    @FeatureScope
    fun provideFavouritesDAppRepository(
        dao: FavouriteDAppsDao
    ): FavouritesDAppRepository = DbFavouritesDAppRepository(dao)
}
