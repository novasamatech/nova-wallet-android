package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_tabs")
data class BrowserTabLocal(
    @PrimaryKey(autoGenerate = false) val id: String,
    val currentUrl: String,
    val creationTime: Long,
    val pageName: String?,
    val pageIconPath: String?,
    val pagePicturePath: String?
)
