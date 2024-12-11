package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.FavouriteDAppsDao
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapFavouriteDAppLocalToFavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapFavouriteDAppToFavouriteDAppLocal
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Null
import kotlinx.coroutines.flow.Flow

interface FavouritesDAppRepository {

    fun observeFavourites(): Flow<List<FavouriteDApp>>

    suspend fun getFavourites(): List<FavouriteDApp>

    suspend fun addFavourite(favouriteDApp: FavouriteDApp)

    fun observeIsFavourite(url: String): Flow<Boolean>

    suspend fun removeFavourite(dAppUrl: String)

    suspend fun updateFavoriteDapps(favoriteDapps: List<FavouriteDApp>)

    suspend fun getNextOrderingIndex(): Int
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

    override suspend fun updateFavoriteDapps(favoriteDapps: List<FavouriteDApp>) {
        val newDapps = favoriteDapps.map { mapFavouriteDAppToFavouriteDAppLocal(it) }
        val currentDapps = favouriteDAppsDao.getFavouriteDApps()
        val diff = CollectionDiffer.findDiff(newDapps, currentDapps, false)
        favouriteDAppsDao.updateFavourites(diff.updated)
    }

    override suspend fun getNextOrderingIndex(): Int {
        return try {
            favouriteDAppsDao.getMaxOrderingIndex() + 1
        } catch (e: NullPointerException) { // For case we don't have added favorite dapps
            0
        }
    }
}
