package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.FavouriteDAppsDao
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapFavouriteDAppLocalToFavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapFavouriteDAppToFavouriteDAppLocal
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import kotlinx.coroutines.flow.Flow

interface FavouritesDAppRepository {

    fun observeFavourites(): Flow<List<FavouriteDApp>>

    suspend fun getFavourites(): List<FavouriteDApp>

    suspend fun addFavourite(favouriteDApp: FavouriteDApp)

    fun observeIsFavourite(url: String): Flow<Boolean>

    suspend fun removeFavourite(dAppUrl: String)
}

class DbFavouritesDAppRepository(
    private val favouriteDAppsDao: FavouriteDAppsDao
) : FavouritesDAppRepository {

    override fun observeFavourites(): Flow<List<FavouriteDApp>> {
        return favouriteDAppsDao.observeFavouriteDApps()
            .mapList(::mapFavouriteDAppLocalToFavouriteDApp)
    }

    override suspend fun getFavourites(): List<FavouriteDApp> {
        return favouriteDAppsDao.getFavouriteDApps()
            .map(::mapFavouriteDAppLocalToFavouriteDApp)
    }

    override suspend fun addFavourite(favouriteDApp: FavouriteDApp) {
        val local = mapFavouriteDAppToFavouriteDAppLocal(favouriteDApp)

        favouriteDAppsDao.insertFavouriteDApp(local)
    }

    override fun observeIsFavourite(url: String): Flow<Boolean> {
        return favouriteDAppsDao.observeIsFavourite(url)
    }

    override suspend fun removeFavourite(dAppUrl: String) {
        favouriteDAppsDao.deleteFavouriteDApp(dAppUrl)
    }
}
