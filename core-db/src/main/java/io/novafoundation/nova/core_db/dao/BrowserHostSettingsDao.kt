package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.BrowserHostSettingsLocal

@Dao
abstract class BrowserHostSettingsDao {

    @Query("SELECT * FROM browser_host_settings")
    abstract suspend fun getBrowserAllHostSettings(): List<BrowserHostSettingsLocal>

    @Query("SELECT * FROM browser_host_settings WHERE hostUrl = :host")
    abstract suspend fun getBrowserHostSettings(host: String): BrowserHostSettingsLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBrowserHostSettings(settings: BrowserHostSettingsLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBrowserHostSettings(settings: List<BrowserHostSettingsLocal>)
}
