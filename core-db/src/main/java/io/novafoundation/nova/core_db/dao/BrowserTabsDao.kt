package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.core_db.model.BrowserTabLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BrowserTabsDao {

    @Query("SELECT id FROM browser_tabs WHERE metaId = :metaId")
    abstract fun getTabIdsFor(metaId: Long): List<String>

    @Query("SELECT * FROM browser_tabs WHERE metaId = :metaId ORDER BY creationTime DESC")
    abstract fun observeTabsByMetaId(metaId: Long): Flow<List<BrowserTabLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTab(tab: BrowserTabLocal)

    @Transaction
    open suspend fun removeTabsByMetaId(metaId: Long): List<String> {
        val tabIds = getTabIdsFor(metaId)
        removeTabsByIds(tabIds)
        return tabIds
    }

    @Query("DELETE FROM browser_tabs WHERE id = :tabId")
    abstract suspend fun removeTab(tabId: String)

    @Query("DELETE FROM browser_tabs WHERE id IN (:tabIds)")
    abstract suspend fun removeTabsByIds(tabIds: List<String>)

    @Query("UPDATE browser_tabs SET pageName = :pageName, pageIconPath = :pageIconPath, pagePicturePath = :pagePicturePath WHERE id = :tabId")
    abstract suspend fun updatePageSnapshot(tabId: String, pageName: String?, pageIconPath: String?, pagePicturePath: String?)

    @Query("UPDATE browser_tabs SET currentUrl = :url WHERE id = :tabId")
    abstract fun updateCurrentUrl(tabId: String, url: String)

    @Query("UPDATE browser_tabs SET dappMetadata_iconLink = :dappIconUrl WHERE id = :tabId")
    abstract fun updateKnownDAppMetadata(tabId: String, dappIconUrl: String?)
}
