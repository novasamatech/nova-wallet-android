package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models


abstract class ManualBackupSecretsVisibilityRvItem : ManualBackupSecretsRvItem {

    abstract var isShown: Boolean
        protected set

    fun makeShown() {
        isShown = true
    }
}
