package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.BrowserTabLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BrowserTabsDao {

    @Query("SELECT * FROM browser_tabs")
    abstract fun observeAllTabs(): Flow<List<BrowserTabLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTab(tab: BrowserTabLocal)

    @Query("DELETE FROM browser_tabs WHERE id = :tabId")
    abstract suspend fun removeTab(tabId: String)

    @Query("DELETE FROM browser_tabs")
    abstract suspend fun removeAllTabs()

    @Query("UPDATE browser_tabs SET pageName = :pageName, pageIconPath = :pageIconPath, pagePicturePath = :pagePicturePath WHERE id = :tabId")
    abstract suspend fun updatePageSnapshot(tabId: String, pageName: String?, pageIconPath: String?, pagePicturePath: String?)

    @Query("UPDATE browser_tabs SET currentUrl = :url WHERE id = :tabId")
    abstract fun updateCurrentUrl(tabId: String, url: String)
}
