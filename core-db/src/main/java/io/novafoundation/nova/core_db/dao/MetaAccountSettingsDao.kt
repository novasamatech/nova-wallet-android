package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.MetaAccountSettingsLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaAccountSettingsDao {

    @Query("SELECT autoAddTokensWithBalance FROM meta_account_settings WHERE metaId = :metaId")
    fun observeAutoAddTokensWithBalance(metaId: Long): Flow<Boolean?>

    @Query("SELECT autoAddTokensWithBalance FROM meta_account_settings WHERE metaId = :metaId")
    suspend fun getAutoAddTokensWithBalance(metaId: Long): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSettings(settings: MetaAccountSettingsLocal)
}
