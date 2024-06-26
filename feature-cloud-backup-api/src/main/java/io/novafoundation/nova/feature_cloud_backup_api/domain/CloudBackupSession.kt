package io.novafoundation.nova.feature_cloud_backup_api.domain

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface CloudBackupSession {

    /**
     * Check if user enabled sync with cloud backup in the current application instance
     * Enabling this means that the app should write changes of local state to backup on addition, modification or delegation of accounts
     */
    suspend fun isSyncWithCloudEnabled(): Boolean

    /**
     * @see isSyncWithCloudEnabled
     */
    suspend fun setSyncingBackupEnabled(enable: Boolean)

    /**
     * Observe the time of the latest backup sync
     */
    fun lastSyncedTimeFlow(): Flow<Date?>

    /**
     * @see lastSyncedTimeFlow
     */
    suspend fun setLastSyncedTime(date: Date)

    /**
     * @throws PasswordNotSaved
     */
    suspend fun getSavedPassword(): Result<String>

    suspend fun setSavedPassword(password: String)

    fun cloudBackupWasInitialized(): Boolean

    fun setBackupWasInitialized()
}

suspend fun CloudBackupSession.setLastSyncedTimeAsNow() {
    setLastSyncedTime(Date())
}

suspend fun CloudBackupSession.initEnabledBackup(password: String) {
    setBackupWasInitialized()
    setSyncingBackupEnabled(true)
    setLastSyncedTimeAsNow()
    setSavedPassword(password)
}
