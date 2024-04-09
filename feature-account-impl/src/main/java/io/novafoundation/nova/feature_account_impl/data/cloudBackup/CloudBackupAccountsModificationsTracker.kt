package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.isProxied

interface CloudBackupAccountsModificationsTracker {

    fun recordAccountsModified(modifiedAccountType: LightMetaAccount.Type)

    fun getAccountsLastModifiedAt(): Long
}

class RealCloudBackupAccountsModificationsTracker(
    private val preferences: Preferences
): CloudBackupAccountsModificationsTracker {

    init {
        ensureInitialized()
    }

    companion object {
        private const val MODIFIED_AT_KEY = "AccountsModificationsTracker.Key"

    }

    override fun recordAccountsModified(modifiedAccountType: LightMetaAccount.Type) {
      if (!modifiedAccountType.isProxied) {
          recordAccountsModified()
      }
    }

    private fun recordAccountsModified() {
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
