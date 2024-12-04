package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.novafoundation.nova.core_db.model.FavouriteDAppLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDAppsDao {

    @Query("SELECT * FROM favourite_dapps")
    fun observeFavouriteDApps(): Flow<List<FavouriteDAppLocal>>

    @Query("SELECT * FROM favourite_dapps")
    suspend fun getFavouriteDApps(): List<FavouriteDAppLocal>

    @Query("SELECT EXISTS(SELECT * FROM favourite_dapps WHERE url = :dAppUrl)")
    fun observeIsFavourite(dAppUrl: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavouriteDApp(dApp: FavouriteDAppLocal)

    @Query("DELETE FROM favourite_dapps WHERE url = :dAppUrl")
    suspend fun deleteFavouriteDApp(dAppUrl: String)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFavourites(dapps: List<FavouriteDAppLocal>)
}
