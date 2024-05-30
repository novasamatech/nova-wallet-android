package io.novafoundation.nova.feature_cloud_backup_impl.data.preferences

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

interface CloudBackupPreferences {

    suspend fun syncWithCloudEnabled(): Boolean

    suspend fun setSyncWithCloudEnabled(enabled: Boolean)

    fun observeLastSyncedTime(): Flow<Date?>

    suspend fun setLastSyncedTime(date: Date)

    suspend fun setSavedPassword(password: String)

    suspend fun getSavedPassword(): String?

    fun getCloudBackupWasInitialized(): Boolean

    fun setCloudBackupWasInitialized(value: Boolean)
}

suspend fun CloudBackupPreferences.enableSyncWithCloud() = setSyncWithCloudEnabled(true)

internal class SharedPrefsCloudBackupPreferences(
    private val preferences: Preferences,
    private val encryptedPreferences: EncryptedPreferences
) : CloudBackupPreferences {

    companion object {
        private const val CLOUD_BACKUP_WAS_INITIALIZED = "CloudBackupPreferences.cloud_backup_was_initialized"
        private const val SYNC_WITH_CLOUD_ENABLED_KEY = "CloudBackupPreferences.sync_with_cloud_enabled"
        private const val LAST_SYNC_TIME_KEY = "CloudBackupPreferences.last_sync_time"
        private const val BACKUP_ENABLED_DEFAULT = false

        private const val PASSWORD_KEY = "CloudBackupPreferences.backup_password"
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

    override suspend fun setLastSyncedTime(date: Date) {
        preferences.putLong(LAST_SYNC_TIME_KEY, date.time)
    }

    override suspend fun setSavedPassword(password: String) {
        encryptedPreferences.putEncryptedString(PASSWORD_KEY, password)
    }

    override suspend fun getSavedPassword(): String? {
        return encryptedPreferences.getDecryptedString(PASSWORD_KEY)
    }

    override fun getCloudBackupWasInitialized(): Boolean {
        return preferences.getBoolean(CLOUD_BACKUP_WAS_INITIALIZED, false)
    }

    override fun setCloudBackupWasInitialized(value: Boolean) {
        preferences.putBoolean(CLOUD_BACKUP_WAS_INITIALIZED, value)
    }
}
