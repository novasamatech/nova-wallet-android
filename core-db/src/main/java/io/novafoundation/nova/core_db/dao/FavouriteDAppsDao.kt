package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.FavouriteDAppLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDAppsDao {

    @Query("SELECT * FROM favourite_dapps")
    fun observeFavouriteDApps(): Flow<List<FavouriteDAppLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavouriteDApp(dApp: FavouriteDAppLocal)

    @Query("DELETE FROM favourite_dapps WHERE url = :dAppUrl")
    suspend fun deleteFavouriteDApp(dAppUrl: String)
}
