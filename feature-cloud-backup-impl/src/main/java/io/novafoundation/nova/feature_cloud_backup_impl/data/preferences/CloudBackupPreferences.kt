package io.novafoundation.nova.feature_cloud_backup_impl.data.preferences

import io.novafoundation.nova.common.data.storage.Preferences
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CloudBackupPreferences {

    suspend fun syncWithCloudEnabled(): Boolean

    suspend fun setSyncWithCloudEnabled(enabled: Boolean)

    fun observeLastSyncedTime(): Flow<Date?>

    fun observeCloudBackupEnabled(): Flow<Boolean>

    suspend fun setLastSyncedTime(date: Date)
}

suspend fun CloudBackupPreferences.enableSyncWithCloud() = setSyncWithCloudEnabled(true)

internal class SharedPrefsCloudBackupPreferences(
    private val preferences: Preferences
) : CloudBackupPreferences {

    companion object {
        private const val SYNC_WITH_CLOUD_ENABLED_KEY = "sync_with_cloud_enabled"
        private const val LAST_SYNC_TIME_KEY = "last_sync_time"
        private const val BACKUP_ENABLED_DEFAULT = false
    }

    override suspend fun syncWithCloudEnabled(): Boolean {
        return preferences.getBoolean(SYNC_WITH_CLOUD_ENABLED_KEY, BACKUP_ENABLED_DEFAULT)
    }

    override suspend fun setSyncWithCloudEnabled(enabled: Boolean) {
        preferences.putBoolean(SYNC_WITH_CLOUD_ENABLED_KEY, enabled)
    }

    override fun observeLastSyncedTime(): Flow<Date?> {
        return preferences.keyFlow(LAST_SYNC_TIME_KEY).map {
            if (preferences.contains(it)) {
                Date(preferences.getLong(it, 0))
            } else {
                null
            }
        }
    }

    override fun observeCloudBackupEnabled(): Flow<Boolean> {
        return preferences.booleanFlow(SYNC_WITH_CLOUD_ENABLED_KEY, BACKUP_ENABLED_DEFAULT)
    }

    override suspend fun setLastSyncedTime(date: Date) {
        preferences.putLong(LAST_SYNC_TIME_KEY, date.time)
    }
}
