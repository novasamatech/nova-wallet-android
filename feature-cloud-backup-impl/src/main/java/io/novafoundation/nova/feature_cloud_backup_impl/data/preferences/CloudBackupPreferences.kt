package io.novafoundation.nova.feature_cloud_backup_impl.data.preferences

import io.novafoundation.nova.common.data.storage.Preferences

interface CloudBackupPreferences {

    suspend fun syncWithCloudEnabled(): Boolean

    suspend fun setSyncWithCloudEnabled(enabled: Boolean)
}

suspend fun CloudBackupPreferences.enableSyncWithCloud() = setSyncWithCloudEnabled(true)

internal class SharedPrefsCloudBackupPreferences(
    private val preferences: Preferences
) : CloudBackupPreferences {

    companion object {
        private const val KEY = "BackupPreferences"
        private const val BACKUP_ENABLED_DEFAULT = false
    }

    override suspend fun syncWithCloudEnabled(): Boolean {
        return preferences.getBoolean(KEY, BACKUP_ENABLED_DEFAULT)
    }

    override suspend fun setSyncWithCloudEnabled(enabled: Boolean) {
        preferences.putBoolean(KEY, enabled)
    }
}
