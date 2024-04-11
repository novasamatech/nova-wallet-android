package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

interface CloudBackupAccountsModificationsTracker {

    fun recordAccountModified(modifiedAccountTypes: List<LightMetaAccount.Type>)

    fun recordAccountModified(modifiedAccountType: LightMetaAccount.Type)

    fun recordAccountsModified()

    fun getAccountsLastModifiedAt(): Long
}

class RealCloudBackupAccountsModificationsTracker(
    private val preferences: Preferences
) : CloudBackupAccountsModificationsTracker {

    init {
        ensureInitialized()
    }

    companion object {
        private const val MODIFIED_AT_KEY = "AccountsModificationsTracker.Key"
    }

    override fun recordAccountModified(modifiedAccountType: LightMetaAccount.Type) {
        if (modifiedAccountType.isBackupable()) {
            recordAccountsModified()
        }
    }

    override fun recordAccountModified(modifiedAccountTypes: List<LightMetaAccount.Type>) {
        if (modifiedAccountTypes.any { it.isBackupable() }) {
            recordAccountsModified()
        }
    }

    override fun recordAccountsModified() {
        preferences.putLong(MODIFIED_AT_KEY, System.currentTimeMillis())
    }

    override fun getAccountsLastModifiedAt(): Long {
        return preferences.getLong(MODIFIED_AT_KEY, System.currentTimeMillis())
    }

    private fun ensureInitialized() {
        if (!preferences.contains(MODIFIED_AT_KEY)) {
            recordAccountsModified()
        }
    }
}
