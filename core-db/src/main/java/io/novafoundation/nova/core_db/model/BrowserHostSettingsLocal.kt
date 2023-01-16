package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable

@Entity(tableName = "browser_host_settings")
data class BrowserHostSettingsLocal(
    @PrimaryKey
    val hostUrl: String,
    val isDesktopModeEnabled: Boolean
) : Identifiable {
    @Ignore
    override val identifier: String = hostUrl
}
